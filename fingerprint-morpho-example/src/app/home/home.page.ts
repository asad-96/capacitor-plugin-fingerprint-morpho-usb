import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
  MorphoUsb,
  FingerprintResponse,
} from 'capacitor-plugin-fingerprint-morpho-usb';
import { ToastService } from '../core/toast/toast.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage implements OnInit {
  openDeviceSuccess = false;
  openDeviceMessage = '';
  capturedFingerprint: FingerprintResponse;
  eventResponse = '';
  fingerPrints: any = [];

  public form: FormGroup;
  constructor(
    private toastSvc: ToastService,
    private changeRef: ChangeDetectorRef,
  ) {
    this.form = new FormGroup({
      userID: new FormControl('', {
        updateOn: 'change',
        validators: [Validators.required],
      }),
      firstName: new FormControl('', null),
      lastName: new FormControl('', null),
    });
  }

  ngOnInit(): void {
    MorphoUsb.addListener('MorphoEvent', data => {
      this.eventResponse = data.info;
      // console.log(data);
    });
  }

  async openDevice() {
    const val = await MorphoUsb.openDevice();
    console.log('Open Device', val.success);
    this.openDeviceSuccess = val.success;
    this.openDeviceMessage = val.error;
    this.changeRef.detectChanges();
  }

  async captureFingerprint() {
    this.capturedFingerprint = undefined;
    this.toastSvc.presentToast('Input Finger');
    this.capturedFingerprint = await MorphoUsb.captureFingerprint({
      userId: this.form.controls.userID.value,
      firstName: this.form.controls.firstName.value,
      lastName: this.form.controls.lastName.value,
    });
    // if (this.capturedFingerprint.success) {
    //   this.fingerPrints.push(this.capturedFingerprint.data);
    // }
    this.changeRef.detectChanges();
  }

  async compareFingerprint() {
    this.capturedFingerprint = undefined;
    // this.toastSvc.presentToast('Input Finger', 500);
    // if (this.fingerPrints.length > 0) {
    // this.toastSvc.presentToast(
    //   'Input Finger: for Prints' + this.fingerPrints.length,
    // );
    this.capturedFingerprint = await MorphoUsb.compareFingerprint({
      userId: this.form.controls.userID.value,
    });
    // this.toastSvc.presentToast(this.ca);
    // } else {
    //   this.toastSvc.presentToast('No template found');
    // }
    this.changeRef.detectChanges();
  }
}
