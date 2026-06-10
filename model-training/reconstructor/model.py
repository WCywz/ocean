"""Point-wise MLP for surface to subsurface reconstruction.

Input:  [B, 7]  surface features + spatial encoding
Output: [B, 36] full depth profiles (thetao[18] + so[18])

Architecture: Residual MLP with LayerNorm - much lighter than U-Net,
suitable for per-point prediction on small grids.
"""
import torch
import torch.nn as nn


class ResidualBlock(nn.Module):
    def __init__(self, dim, dropout=0.1):
        super().__init__()
        self.net = nn.Sequential(
            nn.LayerNorm(dim),
            nn.Linear(dim, dim * 2),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(dim * 2, dim),
            nn.Dropout(dropout),
        )

    def forward(self, x):
        return x + self.net(x)


class DepthProfileMLP(nn.Module):
    """MLP that maps surface observations to full depth profiles.

    Input:  7 features (thetao_sfc, so_sfc, log10_chl, zos, wind_curl, lat, lon)
    Output: 36 values (18 thetao depth levels + 18 so depth levels)
    """

    def __init__(self, in_features=7, n_depths=18, hidden=256, n_blocks=3, dropout=0.15):
        super().__init__()
        self.in_features = in_features
        self.n_depths = n_depths
        out_features = n_depths * 2

        self.input_proj = nn.Sequential(
            nn.Linear(in_features, hidden),
            nn.LayerNorm(hidden),
            nn.GELU(),
            nn.Dropout(dropout),
        )

        self.blocks = nn.Sequential(*[
            ResidualBlock(hidden, dropout) for _ in range(n_blocks)
        ])

        self.head = nn.Sequential(
            nn.LayerNorm(hidden),
            nn.Linear(hidden, hidden // 2),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(hidden // 2, out_features),
        )

    def forward(self, x):
        # x: [B, 7]
        h = self.input_proj(x)
        h = self.blocks(h)
        out = self.head(h)
        return out

    def predict_thetao_so(self, x):
        """Convenience: split output into thetao and so."""
        out = self.forward(x)
        thetao = out[:, :self.n_depths]
        so = out[:, self.n_depths:]
        return thetao, so
