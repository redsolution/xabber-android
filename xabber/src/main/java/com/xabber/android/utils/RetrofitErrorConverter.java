package com.xabber.android.utils;

import android.support.annotation.Nullable;

import com.xabber.android.data.xaccount.HttpApiManager;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * Created by valery.miller on 28.07.17.
 */

public class RetrofitErrorConverter {

    @Nullable
    public static String throwableToHttpError(Throwable throwable) {
        String errorMessage = null;
        APIError error = null;

        if (throwable instanceof HttpException) {
            HttpException exception = (HttpException) throwable;
            Response response = exception.response();
            ResponseBody responseBody = response.errorBody();

            if (responseBody != null) {
                Converter<ResponseBody, APIError> converter = HttpApiManager.getRetrofit().responseBodyConverter(APIError.class, new Annotation[0]);
                try {
                    error = converter.convert(responseBody);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (error != null) {
                if (error.getDetail() != null)
                    errorMessage = error.getDetail();
                else if (error.getEmail() != null && error.getEmail().size() > 0)
                    errorMessage = error.getEmail().get(0);
                else if (error.getCredentials() != null && error.getCredentials().size() > 0)
                    errorMessage = error.getCredentials().get(0);
                else if (error.getCode() != null && error.getCode().size() > 0)
                    errorMessage = error.getCode().get(0);
                else if (error.getUsername() != null && error.getUsername().size() > 0)
                    errorMessage = error.getUsername().get(0);
            }
        }

        return errorMessage;
    }

    class APIError {
        private String detail;
        private ArrayList<String> email;
        private ArrayList<String> credentials;
        private ArrayList<String> code;
        private ArrayList<String> username;

        public APIError(String detail, ArrayList<String> email, ArrayList<String> credentials, ArrayList<String> code, ArrayList<String> username) {
            this.detail = detail;
            this.email = email;
            this.credentials = credentials;
            this.code = code;
            this.username = username;
        }

        public String getDetail() {
            return detail;
        }

        public ArrayList<String> getEmail() {
            return email;
        }

        public ArrayList<String> getCredentials() {
            return credentials;
        }

        public ArrayList<String> getCode() {
            return code;
        }

        public ArrayList<String> getUsername() {
            return username;
        }
    }

}
