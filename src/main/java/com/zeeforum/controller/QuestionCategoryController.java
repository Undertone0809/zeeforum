package com.zeeforum.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zeeforum.pojo.Question;
import com.zeeforum.pojo.QuestionCategory;
import com.zeeforum.service.QuestionCategoryService;
import com.zeeforum.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class QuestionCategoryController {

    @Autowired
    QuestionCategoryService questionCategoryService;
    @Autowired
    QuestionService questionService;

    @GetMapping("/question/category/{cid}/{page}/{limit}")
    public String questionPage(
            @PathVariable int cid,
            @PathVariable int page,
            @PathVariable int limit,
            Model model){

        if (page < 1){
            page = 1;
        }

        // 查询这个分类下的所有问题，获取查询的数据信息
        Page<Question> pageParam = new Page<>(page, limit);
        questionService.page(pageParam,new QueryWrapper<Question>()
                .eq("category_id",cid).orderByDesc("gmt_create"));
        List<Question> records = pageParam.getRecords();
        model.addAttribute("questionList",records);
        model.addAttribute("pageParam",pageParam);

        // 查询这个分类信息
        QuestionCategory category = questionCategoryService.getById(cid);
        model.addAttribute("thisCategoryName",category.getCategory());

        // 全部分类信息
        List<QuestionCategory> categoryList = questionCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "question/list";
    }
}

