import type { PluginListenerHandle } from '@capacitor/core';

export interface MorphoUsbPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  openDevice(): Promise<OpenDeviceResponse>;
  captureFingerprint(userInfo: UserInfo): Promise<FingerprintResponse>;
  compareFingerprint(userdata: UserData): Promise<FingerprintResponse>;

  addListener(
    eventName: 'MorphoEvent',
    listenerFunc: (data: { info: any }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
}

export interface OpenDeviceResponse {
  success: boolean;
  error?: string;
}

export interface FingerprintResponse {
  success: boolean;
  message: string;
  rawBitmap?: any;
  data?: any;
}

export interface UserInfo {
  userId: string;
  firstName: string;
  lastName: string;
}

export interface UserData {
  userId: string;
}
