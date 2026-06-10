"""PhyLSTM forecaster with multi-head attention and multi-horizon output.

Input:  60-day sequence of 3D fields [B, 60, C]
        where C = n_depths*2 + 1 + 2 (thetao depths + so depths + chl + lat + lon)
Output: 4 horizons x full 3D field [B, 4, n_depths*2 + 1]
"""
import torch
import torch.nn as nn


class DepthEncoder(nn.Module):
    """Compress a depth profile (18 values) into a compact representation."""
    def __init__(self, n_depths, hidden=32):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_depths, 64),
            nn.GELU(),
            nn.Dropout(0.2),
            nn.Linear(64, hidden),
            nn.GELU(),
        )

    def forward(self, x):
        return self.net(x)


class PhyLSTMForecaster(nn.Module):
    """Physics-guided LSTM with split surface/deep latent states.

    Latent state is explicitly split:
      - Surface group: chl + encoded(thetao) + encoded(so) + spatial
      - Deep group:   encoded(thetao deep) + encoded(so deep)

    Input:  [B, 60, C] where C = n_depths*2 + 1 + 2
    Output: [B, 4, n_depths*2 + 1]  (1d, 3d, 5d, 7d)
    """

    def __init__(self, n_depths=18, lstm_units=192, heads=8, dropout=0.3):
        super().__init__()
        self.n_depths = n_depths
        self.lstm_units = lstm_units

        self.thetao_encoder = DepthEncoder(n_depths, 32)
        self.so_encoder = DepthEncoder(n_depths, 32)

        # chl(1) + thetao_enc(32) + so_enc(32) + lat/lon(2) = 67
        input_dim = 1 + 32 + 32 + 2

        self.lstm1 = nn.LSTM(input_dim, lstm_units, num_layers=2, batch_first=True, dropout=dropout)
        self.lstm2 = nn.LSTM(lstm_units, lstm_units, num_layers=2, batch_first=True, dropout=dropout)

        self.attn = nn.MultiheadAttention(lstm_units, heads, batch_first=True)
        self.attn_norm = nn.LayerNorm(lstm_units)
        self.attn_dropout = nn.Dropout(dropout)

        self.space_net = nn.Sequential(
            nn.Linear(2, 64), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(64, 32), nn.GELU(),
        )

        self.n_horizons = 4
        out_dim = n_depths * 2 + 1  # thetao[18] + so[18] + chl[1]

        self.head_1d = self._make_head(lstm_units, out_dim, dropout)
        self.head_3d = self._make_head(lstm_units, out_dim, dropout)
        self.head_5d = self._make_head(lstm_units, out_dim, dropout)
        self.head_7d = self._make_head(lstm_units, out_dim, dropout)

    @staticmethod
    def _make_head(in_dim, out_dim, dropout):
        return nn.Sequential(
            nn.Linear(in_dim + 32, 128), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(128, 64), nn.GELU(), nn.Dropout(dropout),
            nn.Linear(64, out_dim),
        )

    def forward(self, x):
        B, T, _ = x.shape

        thetao = x[:, :, :self.n_depths]
        so = x[:, :, self.n_depths:2 * self.n_depths]
        chl = x[:, :, 2 * self.n_depths:2 * self.n_depths + 1]
        space = x[:, :, -2:]

        thetao_flat = thetao.reshape(B * T, self.n_depths)
        so_flat = so.reshape(B * T, self.n_depths)
        thetao_enc = self.thetao_encoder(thetao_flat).reshape(B, T, -1)
        so_enc = self.so_encoder(so_flat).reshape(B, T, -1)

        fused = torch.cat([chl, thetao_enc, so_enc, space[:, :, :2]], dim=-1)

        x_lstm, _ = self.lstm1(fused)
        x_lstm, _ = self.lstm2(x_lstm)

        attn_out, _ = self.attn(x_lstm, x_lstm, x_lstm)
        x_lstm = self.attn_norm(x_lstm + attn_out)
        x_lstm = self.attn_dropout(x_lstm)

        x_pooled = x_lstm.mean(dim=1)
        space_out = self.space_net(space[:, -1, :2])

        combined = torch.cat([x_pooled, space_out], dim=-1)

        out_1d = self.head_1d(combined)
        out_3d = self.head_3d(combined)
        out_5d = self.head_5d(combined)
        out_7d = self.head_7d(combined)

        return torch.stack([out_1d, out_3d, out_5d, out_7d], dim=1)
