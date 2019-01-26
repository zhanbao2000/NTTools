package io.kurumi.ntt.ui.request;

import com.pengrad.telegrambot.*;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.*;
import io.kurumi.ntt.*;

public class AnswerCallback implements AbsResuest {
    
    private AnswerCallbackQuery answer;
    private TelegramBot bot = Constants.bot;

    public AnswerCallback(String id) {

        answer = new AnswerCallbackQuery(id);

    }
    
    public AnswerCallback(TelegramBot bot,String id) {

        this(id);
        this.bot = bot;

    }
    
    public AnswerCallback(CallbackQuery query) {

        this(query.id());

    }
    
    public AnswerCallback(TelegramBot bot,CallbackQuery query) {
        
        this(query);
        this.bot = bot;
        
    }

    
    public AnswerCallback text(String text) {

        answer.text(text);

        return this;

    }

    public AnswerCallback alert(String text) {

        text(text);

        answer.showAlert(true);

        return this;

    }

    public AnswerCallback url(String url) {

        answer.url(url);

        return this;

    }

    public AnswerCallback cacheTime(int sec) {

        answer.cacheTime(sec);

        return this;

    }

    @Override
    public void exec() {

        bot.execute(answer);

    }

    @Override
    public String toWebHookResp() {
       
        return answer.toWebhookResponse();
    
    }
    
}