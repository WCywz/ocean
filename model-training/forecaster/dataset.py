"""DataLoader for PhyLSTM forecaster."""
import os
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader


class ForecasterDataset(Dataset):
    def __init__(self, data_dir, split="train"):
        self.X = np.load(os.path.join(data_dir, f"X_{split}.npy"))
        self.y = np.load(os.path.join(data_dir, f"y_{split}.npy"))

    def __len__(self):
        return len(self.X)

    def __getitem__(self, idx):
        x = torch.from_numpy(self.X[idx]).float()
        y = torch.from_numpy(self.y[idx]).float()
        return x, y


def create_dataloaders(data_dir, batch_size=32):
    paths = [os.path.join(data_dir, f"{prefix}_{split}.npy")
             for split in ("train", "val", "test")
             for prefix in ("X", "y")]
    for p in paths:
        if not os.path.exists(p):
            raise FileNotFoundError(
                f"Forecaster data not found: {p}. Run prepare_forecaster_data.py first."
            )

    train_ds = ForecasterDataset(data_dir, "train")
    val_ds = ForecasterDataset(data_dir, "val")
    test_ds = ForecasterDataset(data_dir, "test")

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=0)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False, num_workers=0)

    return train_loader, val_loader, test_loader
