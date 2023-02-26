import { registerPlugin } from '@capacitor/core';

import type { NextcloudSsoPlugin } from './definitions';

const NextcloudSso = registerPlugin<NextcloudSsoPlugin>('NextcloudSso', {
  web: () => import('./web').then(m => new m.NextcloudSsoWeb()),
});

export * from './definitions';
export { NextcloudSso };
