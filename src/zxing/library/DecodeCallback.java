package zxing.library;

import android.graphics.Bitmap;

import com.google.zxing.Result;

public interface DecodeCallback {
	public void handleBarcode( Result result, Bitmap barcode, float scaleFactor );
}
