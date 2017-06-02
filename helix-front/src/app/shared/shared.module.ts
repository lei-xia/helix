import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';

import { InputDialogComponent } from './dialog/input-dialog/input-dialog.component';
import { DetailHeaderComponent } from './detail-header/detail-header.component';
import { KeyValuePairDirective, KeyValuePairsComponent } from './key-value-pairs/key-value-pairs.component';
import { JsonViewerComponent } from './json-viewer/json-viewer.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    MaterialModule,
    FlexLayoutModule
  ],
  declarations: [
    InputDialogComponent,
    DetailHeaderComponent,
    KeyValuePairDirective,
    KeyValuePairsComponent,
    JsonViewerComponent
  ],
  entryComponents: [
    InputDialogComponent
  ],
  exports: [
    DetailHeaderComponent,
    KeyValuePairDirective,
    KeyValuePairsComponent,
    JsonViewerComponent
  ]
})
export class SharedModule { }