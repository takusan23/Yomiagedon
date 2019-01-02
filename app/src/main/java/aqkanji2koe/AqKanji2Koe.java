//////////////////////////////////////////////////////////////////////
/*!	@class	AqKanji2Koe Ver.3.0

	@brief	AquesTalk用言語処理部

  	漢字かな混じりテキスト->音声記号列

	@author	N.Yamazaki (Aquest)

	@date	2017/11/15	N.Yamazaki	Creation
*/
//  COPYRIGHT (C) 2011 AQUEST CORP.
//////////////////////////////////////////////////////////////////////

package aqkanji2koe;

public class AqKanji2Koe {
	static {
		System.loadLibrary("AqKanji2Koe");
	}
	// 漢字かな交じりのテキストを音声記号列に変換
    public static synchronized native String convert(String dirDic, String kanjiText);
	// 開発ライセンスキー設定
    public static native int setDevKey(String devkey);
}

