package com.hippo.ehviewer.translation;

public class TranslationTaskInfo {
    public long gid;
    public String token;
    public String title;
    public int progress;
    public State state;
    public String thumb;
    public String uploader;
    public float rating;
    public String jobId;
    public boolean downloaded;
    public int processProgress;
    public int translateProgress;
    public boolean downloading;
    public int downloadProgress;
    public boolean singlePage;
    public int pageIndex;
    public int rangeStart;
    public int rangeEnd;
    public String sourcePath;
    public boolean useLlm;

    public enum State {
        Waiting,
        Translating,
        Paused,
        Canceled,
        Completed
    }

    public String stateText() {
        switch (state) {
            case Waiting: return "Waiting";
            case Translating: return "Translating";
            case Paused: return "Paused";
            case Canceled: return "Canceled";
            case Completed: return "Completed";
            default: return "";
        }
    }
}
