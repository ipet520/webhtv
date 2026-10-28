package com.fongmi.android.tv.ui.homepreview;

import com.fongmi.android.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Local fixture values, deliberately unrelated to Site/Vod/TMDB business identities. */
public final class HomePreviewData {
    public record Card(long id, String title, String metadata, String overview,
                       String rating, String quality, int poster, int backdrop, int logo,
                       com.fongmi.android.tv.bean.Vod vod, String image) {
        public Card(long id,String title,String metadata,String overview,String rating,String quality,int poster,int backdrop,int logo) {
            this(id,title,metadata,overview,rating,quality,poster,backdrop,logo,null,"");
        }
        public static Card fromVod(String site, com.fongmi.android.tv.bean.Vod vod) {
            String key=site+"|"+com.fongmi.android.tv.ui.home.NativeHomeBrowseController.identity(vod);
            long id=0xcbf29ce484222325L;for(int i=0;i<key.length();i++)id=(id^key.charAt(i))*0x100000001b3L;
            return new Card(id,vod.getName(),String.join(" · ",java.util.stream.Stream.of(vod.getYear(),vod.getArea(),vod.getTypeName(),vod.getRemarks()).filter(s->!s.isEmpty()).toList()),
                vod.getContent(),"",vod.getRemarks(),0,0,0,vod,vod.getPic());
        }
    }

    private HomePreviewData() {}

    public static List<Card> cards(int count) {
        String[] titles = {"远山来信", "夜航", "林间回声", "不止此刻", "漫长的夏天", "向海而行"};
        int[] posters = {R.drawable.nhp_poster_amber, R.drawable.nhp_poster_blue, R.drawable.nhp_poster_green};
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < Math.max(0, Math.min(count, 40)); i++) {
            String title = titles[i % titles.length] + (i < titles.length ? "" : " · " + (i + 1));
            result.add(new Card(i, title, "本地演示 · 2026 · 更新至20集 · 剧情 / 冒险",
                    "一封未寄出的信，将几位素不相识的旅人带往同一座山城。穿过森林与长夜，他们在重逢中寻找答案。"
                            + (i % 2 == 0 ? "沿途的风景与记忆，慢慢拼出属于彼此的故事。" : ""),
                    (8 + i % 2) + ".1", i % 2 == 0 ? "4K HDR" : "4K", posters[i % 3],
                    R.drawable.nhp_cinema_demo, 0));
        }
        return List.copyOf(result);
    }

    public static final String[] SOURCES = {"演示片库 A", "演示片库 B", "演示片库 C", "演示片库 D", "演示片库 E", "演示片库 F", "演示片库 G", "演示片库 H", "演示片库 I", "演示片库 J", "演示片库 K", "演示片库 L"};
    /** Local-only content boundary: cached by the presentation's category identity. */
    public static List<Card> categoryCards(int category, String label, int count) {
        if (category == 0) return cards(count);
        List<Card> base = cards(count), result = new ArrayList<>();
        int[] images = {R.drawable.nhp_poster_amber, R.drawable.nhp_poster_blue, R.drawable.nhp_poster_green};
        int[] backgrounds = {R.drawable.nhp_cinema_demo, R.drawable.nhp_backdrop_day, R.drawable.nhp_backdrop_night};
        for (int i = 0; i < base.size(); i++) {
            Card card = base.get((i + category) % base.size());
            result.add(new Card(category * 1000L + i, label + " · " + card.title(),
                    "本地演示 · " + label + " · 2026", "本地分类演示：" + label + "。" + card.overview(),
                    card.rating(), card.quality(), images[(i + category) % images.length], backgrounds[category % backgrounds.length], 0));
        }
        return List.copyOf(result);
    }
    public static final String[] SECONDARY = {"全部", "日韩", "国产", "欧美"};
    public static final String[][] FILTERS = {SECONDARY,
            {"全部", "2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015", "2014", "2013", "2012"},
            {"时间", "人气", "评分"}};
}
