import { WebPlugin } from '@capacitor/core';

import type { NextcloudSsoPlugin, SingleSignOnAccount } from './definitions';

export class NextcloudSsoWeb extends WebPlugin implements NextcloudSsoPlugin {
  async chooseAccount(): Promise<SingleSignOnAccount> {
    throw new Error('Not implemented');
  }
}
