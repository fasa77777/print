package com.jetwork.model;

import java.util.HashMap;
import java.util.Map;

/**
 * KICC 단말기의 발급사 코드(R12, 3자리)를 카드사명으로 바꾼다. 코드표는 파일 아래 주석 참고.
 */
public class CardCompanyName {

	/** 발급사 코드 → 카드사명 */
	private static final Map<String, String> NAMES = new HashMap<String, String>();
	static {
		NAMES.put("002", "광주 은행");
		NAMES.put("003", "알리페이");
		NAMES.put("004", "위챗페이");
		NAMES.put("005", "");
		NAMES.put("006", "하나카드");
		NAMES.put("007", "");
		NAMES.put("008", "하나카드");
		NAMES.put("009", "위챗페이");
		NAMES.put("010", "전북 은행");
		NAMES.put("011", "제주 은행");
		NAMES.put("012", "해외은련");
		NAMES.put("013", "SSG머니");
		NAMES.put("014", "NH모바일직불");
		NAMES.put("015", "");
		NAMES.put("016", "국민 카드사");
		NAMES.put("017", "수협 은행");
		NAMES.put("018", "NH농협카드");
		NAMES.put("019", "씨티은행");
		NAMES.put("020", "국가바우처");
		NAMES.put("021", "온누리전자카드");
		NAMES.put("022", "씨티 카드사");
		NAMES.put("023", "아동급식카드(꿈자람)");
		NAMES.put("024", "KTF 전자상품권");
		NAMES.put("025", "KT 월드패스");
		NAMES.put("026", "비씨 카드사");
		NAMES.put("027", "현대 카드사");
		NAMES.put("028", "JCB");
		NAMES.put("029", "신한 카드사");
		NAMES.put("030", "유니온카드(튠머니코리아)");
		NAMES.put("031", "삼성 카드사");
		NAMES.put("045", "롯데카드사");
		NAMES.put("046", "삼성해외아멕스");
		NAMES.put("047", "롯데카드사");
		NAMES.put("048", "해외다이너스");
		NAMES.put("049", "해외 MASTER");
		NAMES.put("050", "해외 VISA");
		NAMES.put("058", "컬러풀드림카드");
		NAMES.put("060", "카카오페이");
		NAMES.put("067", "다날");
	}

	/** 카드사명을 알 수 없을 때 기록하는 이름 */
	public static final String FOREIGN_CARD = "해외카드";

	/**
	 * 카드 결제(신용·체크)용 카드사명. 코드가 없거나 목록에 없는 코드면 "해외카드"로 돌려준다.
	 * (국내 카드사는 모두 발급사 코드가 오므로, 이름을 못 찾는 카드는 해외 카드로 본다)
	 */
	public static String getCardName(String code) {
		String name = getName(code == null ? null : code.trim());
		return name.isEmpty() ? FOREIGN_CARD : name;
	}

	/** 발급사 코드로 카드사명을 찾는다. 모르는 코드면 빈 문자열. */
	public static String getName(String code) {
		String name = code == null ? null : NAMES.get(code);
		return name == null ? "" : name;
	}
}

/**
"발급사 코드"	발급사 명	영문 코드	비             고
001 (a)
002 (b)	광주 은행	KJB	–.제휴사, 신한에서 비씨로 바뀜 (12.06.25), DDC 거래 없이 EDC 처리(2012.06.25)
003 (c)	알리페이	APC	-. 알리페이바코드 신규 추가(14.03.10)
004 (d)	위챗페이	WKRP	-. 위챗페이(KRP) 신규 추가(17.01.04)
005 (e)
006 (f)	하나카드	HNB	"-.매입사명(하나SK카드->하나카드) 변경(14.12.01)(하나/외환카드 통합)
-. 하나카드 시스템통합(15.07.20)"
007 (g)
008 (h)	하나카드	KEC	"-.KEC : 국내, 해외
-.매입사명(외환카드->하나카드) 변경(14.12.11)(하나/외환카드 통합)
-. 하나카드 시스템통합(15.07.20)"
009 (i)	위챗페이	WCT	-. 위챗페이(중국) 신규 추가(17.08.02)
010 (j)	전북 은행	JBB	-. 제휴사, 신한에서 비씨로 바뀜 (09.07.01)
011 (k)	제주 은행	JJB	-. 제휴사, 신한에서 비씨로 바뀜 (13.04.01) DDC 거래 없이 EDC 처리(2013.04.01)
012 (l)	해외은련	CUPS	-. 해외은련 신규 추가(17.03.09)
013 (m)	SSG머니	SSM	-. SSG머니 신규 추가 (16.08.29)
014 (n)	NH모바일직불	COK	-. NH모바일직불 신규 추가 (17.03.30)
016 (p)	국민 카드사	KMC, KMF	-.KMC : 국내,  KMF : 해외
017 (q)	수협 은행	SUH	- 제휴사, 신한에서 비씨로 바뀜 (09.07.01)
018 (r)	NH농협카드	CHH
019 (s)	씨티은행	HMB	–.HMB : 국내, 해외(제휴사 비씨), DDC 거래 없이 EDC 처리(2012.08.16)
020 (t)	국가바우처	KVC	- 국가바우처 추가(12.03.05)
021 (u)	온누리전자카드	ONC	- 온누리상품권 추가(12.04.30)
022 (v)	씨티 카드사	CTB	–.한미은행으로 승인 요청(제휴사 비씨), DDC 거래 없이 EDC 처리(2012.08.16)
023 (w)	아동급식카드(꿈자람)	FVC	- 베스트전자카드 BIN 추가(14.04.22)
024 (x)	KTF 전자상품권	KTF
025 (y)	KT 월드패스	KWP
026 (z)	비씨 카드사	BCC, BCF	-.BCC : 국내, BCF : 해외, CUP : 중국은련
027 (A)	현대 카드사	HDC, HDF	-.HDC : 국내, HDF : 해외
028 (B)	JCB	JCB	-.직승인 하는 카드사로 승인 요청
029 (C)	신한 카드사	LGC, LGF,	-.LGC : 국내, LGF : 해외, LGTAX : 지방세
		LGTAX
030 (D)	유니온카드(튠머니코리아)	TMC	-. 튠머니코리아 유니온카드 추가(13.10.22)
031 (E)	삼성 카드사	SSC, SSF	-.SSC : 국내, SSF : 해외
032 (F)	현대오일뱅크 상품권	HOC
033 (G)	홈플러스 전자상품권	HPC
034 (H)	신세계 백화점 (자사)	SSG	–.한미은행으로 승인 요청(제휴사 비씨), DDC 거래 없이 EDC 처리(2012.08.16)
035 (I)	한화갤러리아 (자사)	HYD	-. 한양유통(구)
036 (J)	한화전자상품권	HE
037 (K)	SK 모네타 캐시	SMC
038 (L)	LG정유(시큐텍)	LGJ
039 (M)	현대 백화점 (자사)	HDD
040 (N)	푸르미코리아	PRC
041 (O)	지드림카드	NHSR	-. 농협참사랑 카드(구)
042 (P)	대구백화점자사	DGD
043 (Q)
044 (R)	농협 전자상품권	AGP
045 (S)	롯데카드사	LDC	-.LDCARD : 국내, 해외
046 (T)	삼성해외아멕스	DYF	-.삼성 카드사로 승인 요청
047 (U)	롯데카드사	AMX	-.롯데 카드사로 승인 요청
048 (V)	해외다이너스	DCC	-.현대 카드사로 승인 요청
049 (W)	해외 MASTER	MTF	-.직승인 하는 카드사로 승인 요청
050 (@)	해외 VISA	VSF	-.직승인 하는 카드사로 승인 요청
058 ( )	컬러풀드림카드	CFD	-.컬러풀드림카드 추가(18.03.12)
060 ( )	카카오페이	KKAO	- 카카오페이 추가(18.04.24)
067 ( )	다날	DNL	- 다날 삼성페이 휴대폰결제 추가 (19.09.30)

 * **/

