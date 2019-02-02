package io.kurumi.nttools.twitter;

import twitter4j.*;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.pengrad.telegrambot.model.Document;
import io.kurumi.nttools.model.Msg;
import io.kurumi.nttools.utils.Markdown;
import io.kurumi.nttools.utils.UserData;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class DataParser {

    private static Document doc;

    public static void process(UserData user, Msg msg) {

        doc = msg.message().document();

        if (doc == null) return;

        switch (doc.fileName()) {

                case "following.js" : processAccounts(user, msg, true);return;
                case "follower.js" : processAccounts(user, msg, false);return;
                case "tweets.js" : processTweets(user, msg); return;

        }

    }

    public static void processTweets(UserData user, Msg msg) {

        LinkedList<TwiAccount> accounts = user.getTwitterAccounts();

        if (accounts.isEmpty()) {

            msg.send("还没有认证账号 无法调用接口 >_<").exec();

            return;

        }

        TwiAccount account = accounts.getFirst();
        Twitter api = account.createApi();

        File doc = msg.getFile();

        File result = new File(msg.fragment.main.dataDir, "/cache/twittr_data_parse/tweets/" + msg.message().document().fileId() + ".html");

        if (result.isFile()) {

            msg.send("这份结果已经被分析过啦 (｡>∀<｡) :");

            msg.sendUpdatingFile();

            msg.sendFile(result);

            return;

        }

        JSONArray json = new JSONArray(StrUtil.subAfter(FileUtil.readUtf8String(doc), " = ", false));

        StringBuilder page = new StringBuilder("# 所有推文 (◦˙▽˙◦)\n");
        
        for (JSONObject obj : (List<JSONObject>)(Object)json) {

            Status s = ObjectUtil.parseStatus(obj.toString(), account);
            
            page.append("\n\n---\n\n");
            
            parseStatus(page,s,api);

        }
        
        String html = Markdown.parsePage("所有推文","# 你的所有推文 (◦˙▽˙◦)\n" + page);

        FileUtil.writeUtf8String(html, result);

        msg.send("分析成功 (｡>∀<｡) : ").exec();

        msg.sendUpdatingFile();

        msg.sendFile(result);

    }

    private static void parseStatus(StringBuilder page, Status s, Twitter api) {

        if (s.getQuotedStatus() != null) {

            page.append("回复给 :\n");

            parseStatus(page, s.getQuotedStatus(), api);

        }

        if (s.isRetweet() && s.isRetweetedByMe()) {

            page.append("你转推了 : \n");

        }

        page.append("[")
            .append(Markdown.encode(s.getUser().getName()))
            .append("](https://twitter.com/")
            .append(s.getUser().getScreenName()).append(")")
            .append(" @").append(s.getUser().getScreenName()).append("\n\n");

        page.append(s.getText()).append("\n");

        for (MediaEntity e :s.getMediaEntities()) {

            page.append("[![](").append(e.getMediaURLHttps()).append(")](").append(e.getExpandedURL()).append(")\n");

        }

        page.append("\n");

        if (s.getRetweetCount() != 0) {

            page.append("转推 : ").append(s.getRetweetCount());

        }

        if (s.getFavoriteCount() != 0) {

            page.append("喜欢 : ").append(s.getFavoriteCount());

        }


    }

    public static void processAccounts(UserData user, Msg msg, boolean friend) {

        LinkedList<TwiAccount> accounts = user.getTwitterAccounts();

        if (accounts.isEmpty()) {

            msg.send("还没有认证账号 无法调用接口 >_<").exec();

            return;

        }

        Twitter api = accounts.getFirst().createApi();

        File doc = msg.getFile();

        File result = new File(msg.fragment.main.dataDir, "/cache/twittr_data_parse/follow_friends/" + msg.message().document().fileId() + ".html");

        if (result.isFile()) {

            msg.send("这份结果已经被分析过啦 (｡>∀<｡) :");

            msg.sendUpdatingFile();

            msg.sendFile(result);

            return;

        }

        JSONArray json = new JSONArray(StrUtil.subAfter(FileUtil.readUtf8String(doc), " = ", false));

        StringBuilder page = new StringBuilder();

        LinkedList<Long> showCache = new LinkedList<>();

        int index = 1;

        try {

            for (JSONObject obj : (List<JSONObject>)(Object)json) {

                long id;

                if (friend) {

                    id = obj.getJSONObject("following").getLong("accountId");
                } else {

                    id = obj.getJSONObject("follower").getLong("accountId");

                }

                if (showCache.size() < 100) {

                    // lookUpUsers 上限 100

                    showCache.add(id);

                } else {

                    long[] ids = ArrayUtil.unWrap(showCache.toArray(new Long[showCache.size()]));

                    showCache.clear();

                    ResponseList<User> users = api.lookupUsers(ids);

                    for (User u : users) {

                        page.append("「").append(index).append("」 ");
                        page.append(TApi.formatUserNameMarkdown(u));
                        page.append("  \n");

                        index ++;

                    }

                }

            }

            if (showCache.size() != 0) {

                long[] ids = ArrayUtil.unWrap(showCache.toArray(new Long[showCache.size()]));

                showCache.clear();

                ResponseList<User> users = api.lookupUsers(ids);

                for (User u : users) {

                    page.append("「").append(index).append("」 ");
                    page.append(TApi.formatUserNameMarkdown(u));
                    page.append("  \n");

                    index ++;

                }

            }

            String html = Markdown.parsePage("所有 " + (friend ? "关注的人" : "关注者"), "# 这是你的结果 (｡>∀<｡)\n" + page.toString());

            FileUtil.writeUtf8String(html, result);

            msg.send("分析成功 (｡>∀<｡) :").exec();

            msg.sendUpdatingFile();

            msg.sendFile(result);

        } catch (TwitterException e) {}

    }

}