package com.zeeforum.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zeeforum.pojo.Comment;
import com.zeeforum.pojo.Question;
import com.zeeforum.pojo.QuestionCategory;
import com.zeeforum.service.CommentService;
import com.zeeforum.service.QuestionCategoryService;
import com.zeeforum.service.QuestionService;
import com.zeeforum.utils.KuangUtils;
import com.zeeforum.vo.QuestionWriteForm;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;


@Controller
public class QuestionController {

    @Autowired
    QuestionCategoryService questionCategoryService;
    @Autowired
    QuestionService questionService;
    @Autowired
    CommentService commentService;


    // 问题列表展示
    @GetMapping("/question")
    public String questionList(Model model){
        System.out.println("[info] question function");
        Page<Question> pageParam = new Page<>(1, 10);
        questionService.page(pageParam,new QueryWrapper<Question>().orderByDesc("gmt_create"));
        // 结果
        List<Question> questionList = pageParam.getRecords();
        model.addAttribute("questionList",questionList);
        model.addAttribute("pageParam",pageParam);

        // 分类信息
        List<QuestionCategory> categoryList = questionCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "question/list";
    }

    @GetMapping("/question/{page}/{limit}")
    public String questionListPage(
            @PathVariable int page,
            @PathVariable int limit,
            Model model){

        if (page < 1){
            page = 1;
        }
        Page<Question> pageParam = new Page<>(page, limit);
        questionService.page(pageParam,new QueryWrapper<Question>().orderByDesc("gmt_create"));

        // 结果
        List<Question> questionList = pageParam.getRecords();
        model.addAttribute("questionList",questionList);
        model.addAttribute("pageParam",pageParam);

        // 分类信息
        List<QuestionCategory> categoryList = questionCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "question/list";
    }

    // 发布问题
    @GetMapping("/question/write")
    public String toWrite(Model model){
        List<QuestionCategory> categoryList = questionCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);
        return "question/write";
    }

    @PostMapping("/question/write")
    public synchronized String write(QuestionWriteForm questionWriteForm){
        // 构建问题对象
        Question question = new Question();

        question.setQid(KuangUtils.getUuid());
        question.setTitle(questionWriteForm.getTitle());
        question.setContent(questionWriteForm.getContent());
        question.setStatus(0);
        question.setSort(0);
        question.setViews(0);

        question.setAuthorId(questionWriteForm.getAuthorId());
        question.setAuthorName(questionWriteForm.getAuthorName());
        question.setAuthorAvatar(questionWriteForm.getAuthorAvatar());

        QuestionCategory category = questionCategoryService.getById(questionWriteForm.getCategoryId());
        question.setCategoryId(questionWriteForm.getCategoryId());
        question.setCategoryName(category.getCategory());
        question.setGmtCreate(KuangUtils.getTime());
        question.setGmtUpdate(KuangUtils.getTime());
        // 存储对象
        questionService.save(question);

        // 重定向到列表页面
        return "redirect:/question";
    }

    // 阅读问题
    @GetMapping("/question/read/{qid}")
    public String read(@PathVariable("qid") String qid,Model model){
        Question question = questionService.getOne(new QueryWrapper<Question>().eq("qid", qid));
        // todo: redis缓存. 防止阅读重复
        question.setViews(question.getViews()+1);
        questionService.updateById(question);
        model.addAttribute("question",question);
        // todo： 查询评论.
        List<Comment> commentList = commentService.list(new QueryWrapper<Comment>().eq("topic_id", qid).orderByDesc("gmt_create"));
        model.addAttribute("commentList",commentList);
        return "question/read";
    }

    // 评论
    @PostMapping("/question/comment/{qid}")
    public String comment(@PathVariable("qid") String qid, Comment comment){
        // 存储评论
        comment.setCommentId(KuangUtils.getUuid());
        comment.setTopicCategory(2);
        comment.setGmtCreate(KuangUtils.getTime());
        commentService.save(comment);
        // 状态改为已解决
        Question question = questionService.getOne(new QueryWrapper<Question>().eq("qid", qid));
        question.setStatus(1);
        questionService.updateById(question);
        // 重定向到列表页面
        return "redirect:/question/read/"+qid;
    }

    // 编辑问题
    @GetMapping("/question/editor/{uid}/{qid}")
    public synchronized String toEditor(@PathVariable("uid") String uid,
                           @PathVariable("qid") String qid,Model model){

        Question question = questionService.getOne(new QueryWrapper<Question>().eq("qid", qid));
        if (!question.getAuthorId().equals(uid)){
            KuangUtils.print("禁止非法编辑");
            return "redirect:/question";
        }

        model.addAttribute("question",question);

        List<QuestionCategory> categoryList = questionCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "question/editor";
    }

    @PostMapping("/question/editor")
    public String editor(Question question){
        Question queryQuestion = questionService.getOne(new QueryWrapper<Question>().eq("qid", question.getQid()));

        queryQuestion.setTitle(question.getTitle());
        queryQuestion.setCategoryId(question.getCategoryId());
        queryQuestion.setContent(question.getContent());
        queryQuestion.setGmtUpdate(KuangUtils.getTime());

        questionService.updateById(queryQuestion);

        return "redirect:/question/read/"+question.getQid();
    }

    // 删除问题
    @GetMapping("/question/delete/{uid}/{qid}")
    public String delete(@PathVariable("uid") String uid,
                         @PathVariable("qid") String qid){

        Question question = questionService.getOne(new QueryWrapper<Question>().eq("qid", qid));
        if (!question.getAuthorId().equals(uid)){
            KuangUtils.print("禁止非法删除");
            return "redirect:/question";
        }
        questionService.removeById(question);

        // 重定向到列表页面
        return "redirect:/question";
    }


    // md 文件上传
    @ApiOperation(value = "md文件上传问题")
    @RequestMapping("/question/write/file/upload")
    @ResponseBody
    public JSONObject fileUpload(@RequestParam(value = "editormd-image-file", required = true) MultipartFile file, HttpServletRequest request) throws IOException {

        //获得SpringBoot当前项目的路径：System.getProperty("user.dir")
        String path = System.getProperty("user.dir")+"/upload/";

        //按照月份进行分类：
        Calendar instance = Calendar.getInstance();
        String month = (instance.get(Calendar.MONTH) + 1)+"月";
        path = path+month;

        File realPath = new File(path);
        if (!realPath.exists()){
            realPath.mkdir();
        }

        //上传文件地址
        KuangUtils.print("上传文件保存地址："+realPath);

        //解决文件名字问题：我们使用uuid;
        String filename = "ks-"+ UUID.randomUUID().toString().replaceAll("-", "");
        String originalFilename = file.getOriginalFilename();
        // KuangUtils.print(originalFilename);
        assert originalFilename != null;
        int i = originalFilename.lastIndexOf(".");
        String suffix = originalFilename.substring(i + 1);

        String outFilename = filename + "."+suffix;
        KuangUtils.print("文件名：" + outFilename);

        //通过CommonsMultipartFile的方法直接写文件（注意这个时候）
        file.transferTo(new File(realPath +"/"+ outFilename));

        //给editormd进行回调
        JSONObject res = new JSONObject();
        res.put("url","/upload/"+month+"/"+ outFilename);
        res.put("success", 1);
        res.put("message", "upload success!");
        KuangUtils.print(res.toJSONString());

        return res;
    }


}

