import * as app from '@nativescript/core/application';
import {Color} from '@nativescript/core/color';

export default function showSnackbar(options) {
  return new Promise((resolve, reject) => {
    options.actionText = options.actionText ? options.actionText : 'Close';
    options.hideDelay = options.hideDelay ? options.hideDelay : 8000;

    const activity = app.android.foregroundActivity || app.android.startActivity;
    const view = activity.findViewById(android.R.id.content);
    const attachToView = view.getChildAt(0);
    const snackbar = com.google.android.material.snackbar.Snackbar.make(attachToView, options.snackText || '', options.hideDelay);

    snackbar.setAction(options.actionText, new android.view.View.OnClickListener({
      onClick: (args) => {
        resolve({
          command: 'Action',
          reason: 'Action',
          event: args,
        });
      },
    }));
    snackbar.setActionTextColor(new Color('#53ba82').android);
    snackbar.getView().setBackgroundColor(new Color('#3e3e3e').android);
    snackbar.show();
  });
}
