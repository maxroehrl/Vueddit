import {Label} from '@nativescript/core/ui/label';

const paint = new android.graphics.Paint();
paint.setStyle(android.graphics.Paint.Style.STROKE);
paint.setColor(android.graphics.Color.parseColor('#282828'));
paint.setStrokeWidth(5);

const factor = 60;

export default class IndentedLabel extends Label {
  createNativeView() {
    const TV = android.widget.TextView.extend({
      depth: 0,
      paddingBottom: 10,
      onDraw(canvas) {
        this.super.setPadding(factor * this.depth + 20, 0, 10, this.paddingBottom);
        this.super.onDraw(canvas);
        for (let i = 1; i <= this.depth; i++) {
          const indent = i * factor;
          canvas.drawLine(indent, 0, indent, this.super.getHeight(), paint);
        }
      },
      setDepth(depth, paddingBottom) {
        this.depth = depth;
        this.paddingBottom = paddingBottom;
      },
    });
    return new TV(this._context);
  }
}
