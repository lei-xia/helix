import { Injectable } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';

import { AlertDialogComponent } from './dialog/alert-dialog/alert-dialog.component';
import { ConfirmDialogComponent } from './dialog/confirm-dialog/confirm-dialog.component';

@Injectable()
export class HelperService {

  constructor(
    protected snackBar: MatSnackBar,
    protected dialog: MatDialog
  ) { }

  showError(message: string) {
    this.dialog.open(AlertDialogComponent, {
      data: {
        title: 'Error',
        message: message
      }
    });
  }

  showSnackBar(message: string) {
    this.snackBar.open(message, 'OK', {
      duration: 2000,
    });
  }

  showConfirmation(message: string) {
    return this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          title: 'Confirmation',
          message: message
        }
      })
      .afterClosed()
      .toPromise();
  }
}
