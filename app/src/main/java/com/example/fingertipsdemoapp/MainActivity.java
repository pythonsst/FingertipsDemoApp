package com.example.fingertipsdemoapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fingertipsdemoapp.remote.APIUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.fingertipsdemoapp.TeacherQuestionActivity.optionAnsPos;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main";
    ProgressBar pogress_bar;
    FloatingActionButton acceptedButton;
    FloatingActionButton rejectedButton;
    ArrayList<QuizQuestion> quizQuestions = new ArrayList<>();
    QuizQuestionWebViewAdapter quizQuestionWebViewAdapter;
    private ViewPager2 viewPager2;
    private ImageView iv_close, iv_correct;
    private TextView tv_total_points, tv_qus_no, tv_earn_points, tvTime;
    private RelativeLayout rl_report;
    private int currentPage = 0;
    private String mSelectChapter;
    private String mSelectedStatus;
    private int mtotalPages = 0;
    private boolean isReqProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager2 = findViewById(R.id.pager);
        iv_close = findViewById(R.id.iv_close);


        acceptedButton = findViewById(R.id.acceptFab);
        rejectedButton = findViewById(R.id.rejectFab);
        tv_total_points = (TextView) findViewById(R.id.tv_total_points);
        tv_qus_no = (TextView) findViewById(R.id.tv_qus_no);
        tv_earn_points = (TextView) findViewById(R.id.tv_earn_points);
        pogress_bar = findViewById(R.id.pogress_bar);
        tvTime = (TextView) findViewById(R.id.tv_time);
        iv_correct = (ImageView) findViewById(R.id.iv_correct);

        Bundle bundle = getIntent().getExtras();
        mSelectedStatus = bundle.getString("STATUS");
        mSelectChapter = bundle.getString("CHAPTER");
        Log.e("testing", "xxxxx: " + mSelectedStatus);
        Log.e("testing", "onCreate: " + mSelectChapter);

        /*List<QuestionModel> questionModels=getListQuestionModel();
        quizQuestionWebViewAdapter = new AwesomePagerAdapter(this,viewPager2,questionModels);
        viewPager2.setAdapter(quizQuestionWebViewAdapter);
*/
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        acceptedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sentReq(true, viewPager2.getCurrentItem());

            }
        });
        rejectedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sentReq(false, viewPager2.getCurrentItem());

            }
        });

        viewPager2.setPageTransformer(new MarginPageTransformer(100));
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int pos, float positionOffset, int positionOffsetPixels) {
                if (pos < quizQuestions.size()) {
                    tv_qus_no.setText("Question " + (pos + 1) + "/" + quizQuestions.size());

                    String questionStatus = String.valueOf(quizQuestions.get(pos).getQuestionStatus());

                    if (questionStatus.equals("1")) {
                        tv_earn_points.setText("Pending");
                        rejectedButton.setVisibility(View.VISIBLE);
                        acceptedButton.setVisibility(View.VISIBLE);
                    } else if (questionStatus.equals("accepted")) {
                        tv_earn_points.setText("Approved");
                        rejectedButton.setVisibility(View.INVISIBLE);
                        acceptedButton.setVisibility(View.INVISIBLE);
                    } else if (questionStatus.equals("2")) {
                        tv_earn_points.setText("Rejected");
                        rejectedButton.setVisibility(View.INVISIBLE);
                        acceptedButton.setVisibility(View.INVISIBLE);
                    }

                    String id = String.valueOf(quizQuestions.get(pos).getId());
                    tv_total_points.setText(id);

                    int total = quizQuestions.size();

                    if (pos >= (total - 15) && !isReqProcessing && currentPage < mtotalPages) {
                        fetchQuestion(currentPage + 1);
                    }
                }
            }
        });


      /*  quizQuestionAdapter = new QuizQuestionAdapter(this, quslist);
        viewPager2.setAdapter(quizQuestionAdapter);
        fetchQuestion(1);*/
        quizQuestionWebViewAdapter = new QuizQuestionWebViewAdapter(this, quizQuestions);
        viewPager2.setAdapter(quizQuestionWebViewAdapter);
        fetchQuestion(1);

    }


    private void sentReq(boolean isAccept, int pos) {
        ConfigURLs configURLs = APIUtil.appConfig();
        QuizQuestion quizQuestion = quizQuestions().get(pos);
        configURLs.getAllresponseAccQuestionIdAndStatus(String.valueOf(quizQuestion.getId()),
                isAccept ? "approve" : "reject").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                if (response.isSuccessful()) {

                    if (isAccept) {
                        quizQuestion.setQuestionStatus("accepted");
                        Toast.makeText(MainActivity.this, " Question Accepted ", Toast.LENGTH_SHORT).show();
                    } else {
                        quizQuestion.setQuestionStatus("2");
                        Toast.makeText(MainActivity.this, " Question Rejected ", Toast.LENGTH_SHORT).show();
                    }

                    int nextPage = viewPager2.getCurrentItem() + 1;
                    quizQuestionWebViewAdapter.notifyDataSetChanged();
                    if (nextPage < quizQuestions().size())
                        scrollQuestionPage(nextPage);
                }

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });

    }

    private void scrollQuestionPage(int nextPage) {

        viewPager2.setCurrentItem(nextPage, true);
    }

    @JavascriptInterface
    public void fetchQuestion(int page) {
        isReqProcessing = true;
        Call<JsonObject> call = APIUtil.appConfig().getQuestion(mSelectChapter, mSelectedStatus, page);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull retrofit2.Response<JsonObject> response) {
                JsonObject jsonObject = response.body();
                //  Log.e("new Json object : "," OBjecgt "+jsonObject.toString());
                isReqProcessing = false;
                if (response.isSuccessful() && jsonObject != null) {
                    currentPage = page;
                    JsonObject questions = jsonObject.getAsJsonObject("data");
                    JsonObject pageObject = questions.getAsJsonObject("page");
                    if (pageObject != null) {
                        //cur_page: "2",
                        //total_records: "485",
                        //total: 20,
                        //total_pages: 25,
                        String total_records = pageObject.get("total_records").getAsString();
                        mtotalPages = pageObject.get("total_pages").getAsInt();
                        tvTime.setText(total_records);
                    }

                    if (questions != null) {
                        JsonArray quArray = questions.getAsJsonArray("result");
                        for (JsonElement element : quArray) {
                            JsonObject objQns = element.getAsJsonObject();
                            QuizQuestion qnsModel = new QuizQuestion();
                            qnsModel.setId(Integer.parseInt(objQns.get("id").getAsString()));
                            qnsModel.setPoint(0);
                            qnsModel.setSpecialType(objQns.get("text_type").getAsString().equalsIgnoreCase("special"));
                            QuizQuestion.QuestionOption[] options = new QuizQuestion.QuestionOption[4];

                            String opt_a = objQns.get("opt_a").isJsonNull() ? "" : objQns.get("opt_a").getAsString();
                            String opt_a_type = objQns.get("opt_a_type").isJsonNull() ? "" : objQns.get("opt_a_type").getAsString();

                            String opt_b = objQns.get("opt_b").isJsonNull() ? "" : objQns.get("opt_b").getAsString();
                            String opt_b_type = objQns.get("opt_b_type").isJsonNull() ? "" : objQns.get("opt_b_type").getAsString();

                            String opt_c = objQns.get("opt_b").isJsonNull() ? "" : objQns.get("opt_c").getAsString();
                            String opt_c_type = objQns.get("opt_c_type").isJsonNull() ? "" : objQns.get("opt_c_type").getAsString();

                            String opt_d = objQns.get("opt_d").isJsonNull() ? "" : objQns.get("opt_d").getAsString();
                            String opt_d_type = objQns.get("opt_d_type").isJsonNull() ? "" : objQns.get("opt_d_type").getAsString();

                            String status = objQns.get("status").isJsonNull() ? "accepted" : objQns.get("status").getAsString();

                            options[0] = new QuizQuestion.QuestionOption(opt_a, opt_a_type);
                            options[1] = new QuizQuestion.QuestionOption(opt_b, opt_b_type);
                            options[2] = new QuizQuestion.QuestionOption(opt_c, opt_c_type);
                            options[3] = new QuizQuestion.QuestionOption(opt_d, opt_d_type);
                            qnsModel.setOptions(options);
                            qnsModel.setQuestionStatus(status);
                            qnsModel.setQuestion(objQns.get("question").getAsString());
                            qnsModel.setAnswer(objQns.get("answer").getAsString());
                            qnsModel.setQuestionExplaination(objQns.get("question_explaination").getAsString());
                            qnsModel.setQuestionExplanationImage(objQns.get("question_explanation_image").getAsString());
                            JsonElement ques_image = objQns.get("ques_image");
                            if (ques_image != null && !ques_image.isJsonNull())
                                qnsModel.setQuestionImage(ques_image.getAsString());

                            qnsModel.setSelectedOptionPos(optionAnsPos(objQns.get("answer").getAsString()));
                            // quizQuestions.add(qnsModel);
                            quizQuestions.add(qnsModel);

                        }
                        quizQuestionWebViewAdapter.notifyDataSetChanged();
                        // quslist.addAll(quizQuestions);
                        //  Log.e("TAG", "question :" + quslist);
                        //  quizQuestionAdapter.notifyDataSetChanged();
                        tv_qus_no.setText("Question:- " + (viewPager2.getCurrentItem() + 1) + "/" + quizQuestions.size());

                    } else {
                        JsonElement message = jsonObject.get("response_message");
                        if (message != null) {
                            String resmessage = message.getAsString();
                        }
                    }

                }

            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }


/*

    @JavascriptInterface
    public List<QuestionModel> getListQuestionModel(){

        List<QuestionModel> questionModelList=new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            JSONArray m_jArry = obj.getJSONArray("ques");
            Log.i("response :",m_jArry.toString());
            ArrayList<HashMap<String, String>> formList = new ArrayList<HashMap<String, String>>();
            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                final String question=jo_inside.getString("question");
                final String question_image=jo_inside.getString("question_image");
                final String optionA = jo_inside.getString("choiceA");
                final String optionA_pic=jo_inside.getString("choiceImageA");
                final int isOptionAisCorrect=jo_inside.getInt("is_rightA");
                final String optionB= jo_inside.getString("choiceB");
                final String optionB_pic=jo_inside.getString("choiceImageB");
                final int isOptionBisCorrect=jo_inside.getInt("is_rightB");
                final String optionC= jo_inside.getString("choiceC");
                final String optionC_pic=jo_inside.getString("choiceImageC");
                final int isOptionCisCorrect=jo_inside.getInt("is_rightC");
                final String optionD =jo_inside.getString("choiceD");
                final String optionD_pic=jo_inside.getString("choiceImageD");
                final int isOptionDisCorrect=jo_inside.getInt("is_rightD");
                final String optionE =jo_inside.getString("choiceE");
                final String optionE_pic=jo_inside.getString("choiceImageE");
                final int isOptionEisCorrect=jo_inside.getInt("is_rightE");

                QuestionModel model=new QuestionModel();
                model.setQuestion(question);
                model.setQuestin_pic(question_image);
                model.setObtion_a(optionA);
                model.setObtion_a_pic(optionA_pic);
                model.setObtion_b(optionB);
                model.setObtion_b_pic(optionB_pic);
                model.setObtion_c(optionC);
                model.setObtion_b_pic(optionC_pic);
                model.setObtion_d(optionD);
                model.setObtion_b_pic(optionD_pic);
                model.setObtion_e(optionE);
                model.setObtion_b_pic(optionE_pic);
                questionModelList.add(model);
                model.setIsAisCorrect(isOptionAisCorrect);
                model.setIsBisCorrect(isOptionBisCorrect);
                model.setIsCisCorrect(isOptionCisCorrect);
                model.setIsDisCorrect(isOptionDisCorrect);
                model.setIsEisCorrect(isOptionEisCorrect);

                Log.e("whole_model", "getListQuestionModel: "+model );
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  questionModelList;
    }
*/

    public ArrayList<QuizQuestion> quizQuestions() {

        return quizQuestions;
    }


}

