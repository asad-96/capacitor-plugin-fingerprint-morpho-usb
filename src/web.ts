import { WebPlugin } from '@capacitor/core';

import type { MorphoUsbPlugin } from './definitions';

export class MorphoUsbWeb extends WebPlugin implements MorphoUsbPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
