import {Label} from '@nativescript/core/ui/label';

const paint = new android.graphics.Paint();
paint.setStyle(android.graphics.Paint.Style.STROKE);
paint.setColor(android.graphics.Color.parseColor('#282828'));
paint.setStrokeWidth(5);

export default class IndentedLabel extends Label {
  createNativeView() {
    const TV = android.widget.TextView.extend({
      depth: 0,
      onDraw(canvas) {
        const factor = 60;
        this.super.setPadding(factor * this.depth + 20, 0, 10, 50);
        this.super.onDraw(canvas);
        for (let i = 1; i <= this.depth; i++) {
          const indent = i * factor;
          canvas.drawLine(indent, 0, indent, this.super.getHeight(), paint);
        }
      },
      setDepth(depth) {
        this.depth = depth;
      },
    });
    return new TV(this._context);
  }
}
