import { NativeScriptConfig } from '@nativescript/core'

export default {
  id: 'de.max.roehrl.vueddit',
  appResourcesPath: 'app/App_Resources',
  android: {
    v8Flags: '--expose_gc',
    markingMode: 'none',
    maxLogcatObjectSize: 9999,
  },
  appPath: 'app',
} as NativeScriptConfig
