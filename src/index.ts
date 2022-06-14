import { registerPlugin } from '@capacitor/core';

import type { MorphoUsbPlugin } from './definitions';

const MorphoUsb = registerPlugin<MorphoUsbPlugin>('MorphoUsb', {
  web: () => import('./web').then(m => new m.MorphoUsbWeb()),
});

export * from './definitions';
export { MorphoUsb };
