import * as app from '@nativescript/core/application';
import {Color} from '@nativescript/core/color';

export default function showSnackbar(options) {
  return new Promise((resolve, reject) => {
    options.actionText = options.actionText ? options.actionText : 'Close';
    options.hideDelay = options.hideDelay ? options.hideDelay : 3000;

    const activity = app.android.foregroundActivity || app.android.startActivity;
    const view = activity.findViewById(android.R.id.content);
    const attachToView = view.getChildAt(0);
    const snackbar = com.google.android.material.snackbar.Snackbar.make(attachToView, options.snackText, options.hideDelay);

    snackbar.setAction(options.actionText, new android.view.View.OnClickListener({
      onClick: (args) => {
        resolve({
          command: 'Action',
          reason: 'Action',
          event: args,
        });
      },
    }));
    if (options.actionTextColor && Color.isValid(options.actionTextColor)) {
      snackbar.setActionTextColor(new Color(options.actionTextColor).android);
    }
    if (options.backgroundColor && Color.isValid(options.backgroundColor)) {
      snackbar.getView().setBackgroundColor(new Color(options.backgroundColor).android);
    }
    snackbar.show();
  });
}
