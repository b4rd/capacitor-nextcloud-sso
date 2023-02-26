import { WebPlugin } from '@capacitor/core';

import type { NextcloudSsoPlugin } from './definitions';

export class NextcloudSsoWeb extends WebPlugin implements NextcloudSsoPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
