import { Injectable } from '@angular/core';
import { ToastController } from '@ionic/angular';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  constructor(private toastCtrl: ToastController) {}

  async presentToast(message: string, duration: number = 5000) {
    const toast = await this.toastCtrl.create({
      message,
      duration,
    });
    toast.present();
  }

  async presentToastWithOptions(
    message: string,
    duration: number = 5000,
    postion: 'bottom' | 'middle' | 'top' = 'bottom',
    cssClass: string = '',
    showCloseBtn: boolean = true,
    closeBtnText: string = 'Close',
  ) {
    const buttons = [];
    if (showCloseBtn) {
      buttons.push({
        side: 'end',
        icon: 'close',
        role: 'cancel',
        text: closeBtnText,
      });
    }
    const toast = await this.toastCtrl.create({
      message,
      position: postion,
      cssClass,
      duration,
      buttons,
    });
    toast.present();
  }
}
