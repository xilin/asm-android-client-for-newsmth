package com.athena.asm.util;

import android.content.Context;
import android.content.res.TypedArray;

public class ResourceUtil {
	public static int getThemedResource(Context context, int attr) {
		TypedArray values = context.obtainStyledAttributes(new int[] {attr});
		return values.getResourceId(0, 0);
	}
}
