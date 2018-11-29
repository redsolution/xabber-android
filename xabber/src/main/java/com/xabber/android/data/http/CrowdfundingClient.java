package com.xabber.android.data.http;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.xaccount.HttpApiManager;

import java.util.List;

import rx.Single;
import rx.functions.Func1;

public class CrowdfundingClient {

    public static Single<Message> getLeader() {
        return HttpApiManager.getCrowdfundingApi().getLeader(getAPIKey())
                .flatMap(new Func1<Message, Single<? extends Message>>() {
                    @Override
                    public Single<? extends Message> call(Message message) {
                        // save to realm
                        return Single.just(message);
                    }
                });
    }

    public static Single<List<Message>> getFeed() {
        return HttpApiManager.getCrowdfundingApi().getFeed(getAPIKey())
                .flatMap(new Func1<List<Message>, Single<? extends List<Message>>>() {
                    @Override
                    public Single<? extends List<Message>> call(List<Message> messages) {
                        // save to realm
                        return Single.just(messages);
                    }
                });
    }

    private static String getAPIKey() {
        return "APIKey " + Application.getInstance().getResources().getString(R.string.CROWDFUNDING_KEY);
    }

    public static class Message {
        private final String uuid;
        private final String timestamp;
        private final List<Locale> feed;

        public Message(String uuid, String timestamp, List<Locale> feed) {
            this.uuid = uuid;
            this.timestamp = timestamp;
            this.feed = feed;
        }

        public String getUuid() {
            return uuid;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public List<Locale> getFeed() {
            return feed;
        }
    }

    private static class Locale {
        private final String locale;
        private final String message;

        public Locale(String locale, String message) {
            this.locale = locale;
            this.message = message;
        }

        public String getLocale() {
            return locale;
        }

        public String getMessage() {
            return message;
        }
    }
}