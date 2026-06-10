"""DataLoader for reconstructor training (time-based split, not random)."""
import os
import numpy as np
import torch
from torch.utils.data import Dataset, DataLoader


class ReconstructorDataset(Dataset):
    def __init__(self, data_dir, split="train"):
        self.X = np.load(os.path.join(data_dir, "X_surface.npy"))
        self.y_thetao = np.load(os.path.join(data_dir, "y_thetao.npy"))
        self.y_so = np.load(os.path.join(data_dir, "y_so.npy"))
        self.indices = np.load(os.path.join(data_dir, f"{split}_idx.npy"))

    def __len__(self):
        return len(self.indices)

    def __getitem__(self, idx):
        i = self.indices[idx]
        x = torch.from_numpy(self.X[i]).float()
        y_thetao = torch.from_numpy(self.y_thetao[i]).float()
        y_so = torch.from_numpy(self.y_so[i]).float()
        return x, y_thetao, y_so


def create_dataloaders(data_dir, batch_size=256, num_workers=0):
    train_ds = ReconstructorDataset(data_dir, "train")
    val_ds = ReconstructorDataset(data_dir, "val")
    test_ds = ReconstructorDataset(data_dir, "test")

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True, num_workers=num_workers)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False, num_workers=num_workers)

    return train_loader, val_loader, test_loader
