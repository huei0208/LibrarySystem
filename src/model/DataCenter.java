/**
 * 
 */
package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataCenter {
    // 全域共享的清單
    public static List<Book> allBooks = new ArrayList<>();
    public static List<BorrowRecord> allBorrowRecords = new ArrayList<>();

    // 靜態初始化：當程式啟動時自動載入資料
    static {
        initializeBooks();
        initializeRecords();
    }

    private static void initializeBooks() {
    	allBooks.add(createBook("新時代倫理批判分析", "Walter Isaacson", "資訊科技, 電腦科學", "New York : 聯經出版", "2026", "Hardcover", "385面 : 彩圖 ; 25公分", "學術資料庫", new String[]{"9780095734071"}, "含索引及參考文獻。"));
        allBooks.add(createBook("未來城市實務指南", "林美惠, 蘇雅婷, 張志強", "資訊科技, 電腦科學", "London : 聯經出版", "2014", "二版", "308面 : 彩圖 ; 22公分", "學術資料庫", new String[]{"9784792968473", "9795511337819"}, "含索引及參考文獻。"));
        allBooks.add(createBook("國際商事法實務指南", "Brene Brown", "資訊科技, 電腦科學", "台北 : 聯經出版", "2011", "Hardcover", "392面 : 黑白插圖 ; 24公分", "圖書館目錄", new String[]{"9789278366558"}, "含索引及參考文獻。"));
        allBooks.add(createBook("核心能源實務指南", "蘇雅婷, 嚴長壽", "資訊科技, 電腦科學", "Tokyo : Pearson", "2025", "Revised Edition", "316面 : 圖表 ; 21公分", "學術資料庫", new String[]{"9785208919785"}, "含索引及參考文獻。"));
        allBooks.add(createBook("Design as Art", "詹宏志, Yuval Noah Harari", "資訊科技, 電腦科學", "台北 : Springer", "2021", "Hardcover", "334面 : 圖表 ; 22公分", "學術資料庫", new String[]{"9799436312035", "9797795550567"}, "含索引及參考文獻。"));
        allBooks.add(createBook("量子物理學導論", "嚴長壽", "資訊科技, 電腦科學", "Tokyo : Pearson", "2021", "Hardcover", "415面 : 圖表 ; 22公分", "圖書館目錄", new String[]{"9783051380606", "9788901981466"}, "含索引及參考文獻。"));
        allBooks.add(createBook("The Architecture of Silence", "王大明", "資訊科技, 電腦科學", "Tokyo : Springer", "2021", "初版", "386面 : 圖表 ; 25公分", "圖書館目錄", new String[]{"9780971537014", "9791349462313", "9789859332301"}, "含索引及參考文獻。"));
        allBooks.add(createBook("互動創意實務指南", "詹宏志, Brene Brown", "資訊科技, 電腦科學", "London : Penguin", "2011", "Hardcover", "652面 : 彩圖 ; 21公分", "學術資料庫", new String[]{"9786954329759", "9788409599074"}, "含索引及參考文獻。"));
        allBooks.add(createBook("解構倫理批判分析", "Jordan Peterson", "資訊科技, 電腦科學", "Tokyo : Springer", "2010", "Revised Edition", "358面 : 黑白插圖 ; 25公分", "圖書館目錄", new String[]{"9795309834601", "9794975419850"}, "含索引及參考文獻。"));
        allBooks.add(createBook("未來環境案例研究", "嚴長壽, 吳明益, Jordan Peterson", "資訊科技, 電腦科學", "New York : Springer", "2013", "Revised Edition", "787面 : 圖表 ; 26公分", "圖書館目錄", new String[]{"9789851195236", "9782182294682", "9794623741895"}, "含索引及參考文獻。"));
        allBooks.add(createBook("雲端原生應用開發", "Brene Brown, 蔣勳, 蘇雅婷", "資訊科技, 電腦科學", "New York : Pearson", "2012", "二版", "227面 : 黑白插圖 ; 26公分", "學術資料庫", new String[]{"9793895750301", "9789897615095", "9799086276499"}, "含索引及參考文獻。"));
        allBooks.add(createBook("烏合之眾：大眾心理研究", "吳明益", "資訊科技, 電腦科學", "New York : Springer", "2026", "二版", "686面 : 彩圖 ; 22公分", "圖書館目錄", new String[]{"9798882859022", "9782135506327"}, "含索引及參考文獻。"));
        allBooks.add(createBook("全球交通技術手冊", "林美惠", "資訊科技, 電腦科學", "New York : 聯經出版", "2023", "Hardcover", "379面 : 黑白插圖 ; 24公分", "出版社數據", new String[]{"9797288895786", "9793675325781"}, "含索引及參考文獻。"));
        allBooks.add(createBook("都市社會學：空間與權力", "吳明益, Geoffrey Hinton, 唐鳳", "資訊科技, 電腦科學", "New York : Pearson", "2025", "二版", "495面 : 圖表 ; 25公分", "學術資料庫", new String[]{"9793223313331", "9789627317151", "9790940917345"}, "含索引及參考文獻。"));
        allBooks.add(createBook("經典創意案例研究", "John Smith, 蘇雅婷, Walter Isaacson", "資訊科技, 電腦科學", "Tokyo : Springer", "2025", "精裝本", "306面 : 彩圖 ; 25公分", "學術資料庫", new String[]{"9789698269767"}, "含索引及參考文獻。"));
        allBooks.add(createBook("解構能源批判分析", "蘇絢慧, 蘇雅婷", "資訊科技, 電腦科學", "Tokyo : Penguin", "2018", "二版", "706面 : 黑白插圖 ; 24公分", "學術資料庫", new String[]{"9798371334160", "9783322233648", "9781251377987"}, "含索引及參考文獻。"));
        allBooks.add(createBook("Functional Programming in Scala", "Jordan Peterson, 林美惠", "資訊科技, 電腦科學", "台北 : Pearson", "2021", "二版", "503面 : 圖表 ; 23公分", "出版社數據", new String[]{"9780318505402", "9790071482124", "9799072303887"}, "含索引及參考文獻。"));
        allBooks.add(createBook("核心倫理技術手冊", "張志強", "資訊科技, 電腦科學", "New York : 聯經出版", "2017", "初版", "747面 : 黑白插圖 ; 26公分", "學術資料庫", new String[]{"9785480067822", "9791962477982", "9797938872348"}, "含索引及參考文獻。"));
        allBooks.add(createBook("當代政治哲學導論", "吳明益, 張志強", "資訊科技, 電腦科學", "New York : 旗標", "2014", "二版", "226面 : 彩圖 ; 24公分", "出版社數據", new String[]{"9789477163796", "9792618557586"}, "含索引及參考文獻。"));
        allBooks.add(createBook("The Lean Startup", "唐鳳, 王大明, 蘇雅婷", "資訊科技, 電腦科學", "台北 : Pearson", "2018", "Revised Edition", "797面 : 黑白插圖 ; 25公分", "出版社數據", new String[]{"9782873333715"}, "含索引及參考文獻。"));
        
    }

    private static void initializeRecords() {
        int rId = 1;
        // --- 已歸還紀錄 ---
        allBorrowRecords.add(new BorrowRecord(rId++, 5, 23, -45, -38, -42, 7));
        allBorrowRecords.add(new BorrowRecord(rId++, 12, 78, -28, -27, -29, 1));
        // ... (其餘借還紀錄)
        allBorrowRecords.add(new BorrowRecord(rId++, 19, 104, -19, -12, null, 7));
        System.out.println("系統資料初始化完成，共載入 " + allBooks.size() + " 本書及 " + (rId-1) + " 筆紀錄。");
    }

    private static Book createBook(String title, String authors, String subjects, String pub, String year, String ed, String format, String src, String[] ids, String note) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthors(authors);
        b.setSubjects(subjects);
        b.setPublisher(pub);
        b.setPublishYear(year);
        b.setEdition(ed);
        b.setFormatDesc(format);
        b.setSource(src);
        b.setNote(note);
        b.setIsbnList(Arrays.asList(ids)); 
        b.setBookId(ids[0].hashCode() & 0xfffffff);
        b.setStatus(Book.Status.AVAILABLE);
        return b;
    }
}