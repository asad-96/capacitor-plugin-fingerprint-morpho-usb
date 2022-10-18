import { WebPlugin } from '@capacitor/core';

import type {
  FingerprintResponse,
  MorphoUsbPlugin,
  OpenDeviceResponse,
  UserData,
  UserInfo,
} from './definitions';

export class MorphoUsbWeb extends WebPlugin implements MorphoUsbPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async openDevice(): Promise<OpenDeviceResponse> {
    return { success: true, error: 'Not Implemented in web' };
  }

  async captureFingerprint(userInfo: UserInfo): Promise<FingerprintResponse> {
    return {
      success: true,
      message: 'not Implemented in web' + userInfo.userId,
    };
  }

  async compareFingerprint(userdata: UserData): Promise<FingerprintResponse> {
    return {
      success: true,
      message: 'not Implemented in web',
      data: userdata.userId,
    };
  }
}
