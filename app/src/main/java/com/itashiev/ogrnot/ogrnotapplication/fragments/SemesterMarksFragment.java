package com.itashiev.ogrnot.ogrnotapplication.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.itashiev.ogrnot.ogrnotapplication.R;
import com.itashiev.ogrnot.ogrnotapplication.adapter.SemesterLessonsMarksAdapter;
import com.itashiev.ogrnot.ogrnotapplication.model.grade.Exam;
import com.itashiev.ogrnot.ogrnotapplication.model.grade.Grade;
import com.itashiev.ogrnot.ogrnotapplication.model.grade.Lesson;
import com.itashiev.ogrnot.ogrnotapplication.rest.OgrnotApiClient;
import com.itashiev.ogrnot.ogrnotapplication.rest.OgrnotApiInterface;
import com.itashiev.ogrnot.ogrnotapplication.view.EmptyRecyclerView;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SemesterMarksFragment extends HelperFragment {

    private Call<Grade> call;

    private ProgressBar semesterLessonsMarksProgressBar;
    private EmptyRecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private View inflate;
    private static final String TAG = "SemesterMarksFragment";

    public SemesterMarksFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflate = inflater.inflate(R.layout.fragment_semester_marks, container, false);
        recyclerView = (EmptyRecyclerView) inflate.findViewById(R.id.semester_lessons_marks_recycler_view);
        semesterLessonsMarksProgressBar = (ProgressBar) inflate.findViewById(R.id.semester_lessons_marks_progressbar);

        getLessonsMarksFromApi();

        return inflate;
    }

    @Override
    public void onStop() {
        if (call != null) {
            call.cancel();
        }
        super.onStop();
    }

    private void getLessonsMarksFromApi() {
        OgrnotApiInterface apiService = OgrnotApiClient
                .getClient(getActivity().getApplicationContext())
                .create(OgrnotApiInterface.class);

        call = apiService.getGrade();
        call.enqueue(new Callback<Grade>() {
            @Override
            public void onResponse(Call<Grade> call, Response<Grade> response) {
                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    startLoginActivity();
                    return;
                }

                if (call.isExecuted() && response.isSuccessful()) {
                    Grade grade = response.body();
                    fillExamsView(grade);
                    Log.d(TAG, "onResponse: " + grade);
                } else {
                    Log.d(TAG, "onResponse: " + response.raw());
                }

                semesterLessonsMarksProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<Grade> call, Throwable t) {
                if (!call.isCanceled()) {
                    semesterLessonsMarksProgressBar.setVisibility(View.INVISIBLE);
                    showNoInternetConnectionToast();
                }
                Log.e(TAG, "onFailure: " + call.request(), t);
            }
        });
    }

    private void fillExamsView(Grade grade) {
        List<Lesson> lessons = grade.getLessons();
        if (grade.getLessons() == null) {
            lessons = new ArrayList<>();
        }
        List<Lesson> newLessons = getGroupedLessons(lessons);
        Context context = getActivity().getApplicationContext();
        adapter = new SemesterLessonsMarksAdapter(context, newLessons);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(true);

        View emptyView = inflate.findViewById(R.id.semester_lessons_marks_empty_view);
        recyclerView.setEmptyView(emptyView);
    }

    @NonNull
    private List<Lesson> getGroupedLessons(List<Lesson> lessons) {
        Map<String, List<Exam>> map = new LinkedHashMap<>();
        for (Lesson lesson : lessons) {
            if (map.containsKey(lesson.getName())) {
                map.get(lesson.getName()).addAll(lesson.getExams());
            } else {
                map.put(lesson.getName(), lesson.getExams());
            }
        }

        List<Lesson> newLessons = new ArrayList<>();
        for (String name : map.keySet()) {
            newLessons.add(new Lesson(name, map.get(name)));
        }
        return newLessons;
    }
}
