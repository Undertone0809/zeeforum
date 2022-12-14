package com.zeeforum.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zeeforum.pojo.Blog;
import com.zeeforum.pojo.BlogCategory;
import com.zeeforum.pojo.Comment;
import com.zeeforum.service.BlogCategoryService;
import com.zeeforum.service.BlogService;
import com.zeeforum.service.CommentService;
import com.zeeforum.utils.KuangUtils;
import com.zeeforum.vo.QuestionWriteForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
public class BlogController {

    @Autowired
    BlogCategoryService blogCategoryService;
    @Autowired
    BlogService blogService;
    @Autowired
    CommentService commentService;

    // 列表展示
    @GetMapping("/blog")
    public String blogList(Model model){

        Page<Blog> pageParam = new Page<>(1, 10);
        blogService.page(pageParam,new QueryWrapper<Blog>().orderByDesc("gmt_create"));
        // 结果
        List<Blog> blogList = pageParam.getRecords();
        model.addAttribute("blogList",blogList);
        model.addAttribute("pageParam",pageParam);

        // 分类信息
        List<BlogCategory> categoryList = blogCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "blog/list";
    }

    @GetMapping("/blog/{page}/{limit}")
    public String blogListPage(
            @PathVariable int page,
            @PathVariable int limit,
            Model model){

        if (page < 1){
            page = 1;
        }
        Page<Blog> pageParam = new Page<>(page, limit);
        blogService.page(pageParam,new QueryWrapper<Blog>().orderByDesc("gmt_create"));

        // 结果
        List<Blog> blogList = pageParam.getRecords();
        model.addAttribute("blogList",blogList);
        model.addAttribute("pageParam",pageParam);

        // 分类信息
        List<BlogCategory> categoryList = blogCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "blog/list";
    }

    // 写文章
    @GetMapping("/blog/write")
    public String toWrite(Model model){
        List<BlogCategory> categoryList = blogCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);
        return "blog/write";
    }

    @PostMapping("/blog/write")
    public synchronized String write(QuestionWriteForm questionWriteForm){
        // 构建问题对象
        Blog blog = new Blog();

        blog.setBid(KuangUtils.getUuid());
        blog.setTitle(questionWriteForm.getTitle());
        blog.setContent(questionWriteForm.getContent());
        blog.setSort(0);
        blog.setViews(0);

        blog.setAuthorId(questionWriteForm.getAuthorId());
        blog.setAuthorName(questionWriteForm.getAuthorName());
        blog.setAuthorAvatar(questionWriteForm.getAuthorAvatar());

        BlogCategory category = blogCategoryService.getById(questionWriteForm.getCategoryId());
        blog.setCategoryId(questionWriteForm.getCategoryId());
        blog.setCategoryName(category.getCategory());
        blog.setGmtCreate(KuangUtils.getTime());
        blog.setGmtUpdate(KuangUtils.getTime());
        // 存储对象
        blogService.save(blog);

        // 重定向到列表页面
        return "redirect:/blog";
    }

    // 阅读文章
    @GetMapping("/blog/read/{bid}")
    public String read(@PathVariable("bid") String bid,Model model){
        Blog blog = blogService.getOne(new QueryWrapper<Blog>().eq("bid", bid));
        // todo: redis缓存. 防止阅读重复
        blog.setViews(blog.getViews()+1);
        blogService.updateById(blog);
        model.addAttribute("blog",blog);
        // todo： 查询评论
        List<Comment> commentList = commentService.list(new QueryWrapper<Comment>().eq("topic_id", bid).orderByDesc("gmt_create"));
        model.addAttribute("commentList",commentList);
        return "blog/read";
    }

    // 编辑问题
    @GetMapping("/blog/editor/{uid}/{bid}")
    public synchronized String toEditor(@PathVariable("uid") String uid,
                           @PathVariable("bid") String bid,Model model){

        Blog blog = blogService.getOne(new QueryWrapper<Blog>().eq("bid", bid));

        if (!blog.getAuthorId().equals(uid)){
            KuangUtils.print("禁止非法编辑");
            return "redirect:/blog";
        }

        model.addAttribute("blog",blog);

        List<BlogCategory> categoryList = blogCategoryService.list(null);
        model.addAttribute("categoryList",categoryList);

        return "blog/editor";
    }

    @PostMapping("/blog/editor")
    public String editor(Blog blog){
        Blog queryBlog = blogService.getOne(new QueryWrapper<Blog>().eq("bid", blog.getBid()));

        queryBlog.setTitle(blog.getTitle());
        queryBlog.setCategoryId(blog.getCategoryId());
        queryBlog.setContent(blog.getContent());
        queryBlog.setGmtUpdate(KuangUtils.getTime());

        blogService.updateById(queryBlog);

        return "redirect:/blog/read/"+blog.getBid();
    }

    // 删除问题
    @GetMapping("/blog/delete/{uid}/{bid}")
    public String delete(@PathVariable("uid") String uid,
                         @PathVariable("bid") String bid){

        Blog blog = blogService.getOne(new QueryWrapper<Blog>().eq("bid", bid));

        if (!blog.getAuthorId().equals(uid)){
            KuangUtils.print("禁止非法删除");
            return "redirect:/blog";
        }

        blogService.removeById(blog);
        // 重定向到列表页面
        return "redirect:/blog";
    }

    // 评论
    @PostMapping("/blog/comment/{bid}")
    public String comment(@PathVariable("bid") String bid, Comment comment){
        // 存储评论
        comment.setCommentId(KuangUtils.getUuid());
        comment.setTopicCategory(1);
        comment.setGmtCreate(KuangUtils.getTime());
        commentService.save(comment);
        // 重定向到列表页面
        return "redirect:/blog/read/"+bid;
    }




}

