package ru.geek.news_portal.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.geek.news_portal.base.entities.*;
import ru.geek.news_portal.dto.ArticleDto;
import ru.geek.news_portal.services.*;
import ru.geek.news_portal.utils.ierarhy_comments.Tree;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Author Farida Gareeva
 * Created 21/03/2020
 * контроллер для вывода страницы со статьей
 * v1.0
 */

@Controller
@RequestMapping("/single/articles")
public class ArticleController {

    private ArticleService articleService;
    private CommentService commentService;
    private ArticleLikeService articleLikeService;
    private ArticleCategoryService articleCategoryService;
    private CommentLikeService commentLikeService;
    private TagsServiceImpl tagsService;

    //Временное решение до появления сервиса предпочтений пользователя
    private Long RECOMENDED_NEWS = 5L;


    @Autowired
    public ArticleController(ArticleService articleService,
                             CommentService commentService,
                             ArticleLikeService articleLikeService,
                             ArticleCategoryService articleCategoryService,
                             CommentLikeService commentLikeService,
                             TagsServiceImpl tagsService) {
        this.articleService = articleService;
        this.commentService = commentService;
        this.articleLikeService = articleLikeService;
        this.articleCategoryService = articleCategoryService;
        this.commentLikeService = commentLikeService;
        this.tagsService = tagsService;
    }

    /**
     * Updated by Stanislav Ryzhkov 28/03/2020
     * */
   

    @Autowired
    public void setArticleCategoryService(ArticleCategoryService articleCategoryService) {
        this.articleCategoryService = articleCategoryService;
    }

   @GetMapping("/{id}")
    public String showSinglePage(Model model, @PathVariable(value = "id", required = false) Long id) {
        if (id == null) {
            return "ui/404";
        }
       try {

            Tree<Long, Comment> tree = commentService.getCommentsTreeByArticle_id(id);

            ArticleDto article = articleService.findArticleDtoById(id);
//            Article article = articleService.findById(id);
            model.addAttribute("article", article);
            model.addAttribute("articles", articleService.findAllArticles());
            model.addAttribute("comments", tree.getChildren(null));
            model.addAttribute("tree_comments", tree);
//            model.addAttribute("comments", commentService.findAllCommentByArticle_id(id));
            model.addAttribute("comment", new Comment());
            model.addAttribute("categories", articleCategoryService.findAll());
            model.addAttribute("articleLikes", articleLikeService.getArticleLikes(id));
            model.addAttribute("articleDislikes", articleLikeService.getArticleDislikes(id));
            return "ui/single";
        } catch (Exception e) {
            e.printStackTrace();
            return "ui/404";
        }
    }

    /**
     * @author Ostrovskiy Dmitriy
     * @created 17/04/2020
     * Контроллер для сохранения и просмотра созданной статьи
     * v1.0
     */
    @PostMapping({"/editor_article","/editor_article/{id}"} )
    public String saveArticle(Model model, @RequestParam Map<String, String> params, Optional<Article> article,
                              HttpServletRequest request, HttpServletResponse response,
                              @PathVariable(value = "art_id", required = false) Long artId,
                              @RequestParam (value = "mainPicture", required = false) String mainPicture,
                              @RequestParam (value = "tag_id", required = false) ArrayList<String> tagIdArr,
                              @RequestParam (value = "tags", required = false) ArrayList<String> tagsArr,
                              @RequestParam (value = "firstName", required = false) String firstName,
                              @RequestParam (value = "lastName", required = false) String lastName) {

        ArticleCategory category = null;
        List<Tag> tags = new ArrayList<>();
        Long articleId = null;
        if (article.get().getId()!=null) {
            articleId = article.get().getId();
        }

        if (params.containsKey("cat_id")||params.containsKey("category")) {
            Long categoryId = 0L;
            if (params.get("cat_id")!=null) {
                categoryId = Long.parseLong(params.get("cat_id"));
            } else {
                categoryId = Long.parseLong(params.get("category"));
            }
            category = articleCategoryService.findOneById(categoryId);
        }
        if (params.containsKey("art_id")) {
            article = articleService.findByUpdateId(Long.parseLong(params.get("art_id")));
        }
        article.get().getId();
//        List<ArticleDto> articles =  articleService.findAllArticles();
        List<ArticleDto> articles =  articleService.findAllDtoArticles();
        List<ArticleCategory> categories = articleCategoryService.findAll();
        if (tagsArr!=null){
            for (int i = 0; i < tagsArr.size(); i++) {
                tags.add(tagsService.findById(Long.parseLong(tagsArr.get(i))));
            }
            article.get().setTags(tags);
        }

        String url = null;
        if (mainPicture.contains(":")) {
            url = mainPicture;
        } else {
            url = "http://localhost:8199/news/";
        }

        article.get().setMainPictureUrl("<img src=\"" + url + mainPicture + "\"/>");
        article.get().setAuthor(firstName + " " + lastName);
        articleService.save(article.get());
        Long id = article.get().getId();

        model.addAttribute("articles", articles);
        model.addAttribute("articleEdit", article);
        model.addAttribute("categories", categories);
        model.addAttribute("category", category);
        return "redirect:/single/articles/" + id;
    }

    @PostMapping("/comment/{article_id}")
    public String addComment(@PathVariable("article_id") Long article_id,
                             @RequestParam(value = "id_parent", required = false) Long id_parent,
                             @ModelAttribute("comment") Comment comment,
                             BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "ui/404";
        }
        commentService.fillAndSaveComment(comment, article_id, request.getRemoteUser(), id_parent);
        return "redirect:/single/articles/" + article_id;
    }

    @GetMapping("/add/like/{id}")
    public String addArticleLike(@PathVariable("id") Long article_id,
                                 Principal principal) {

        articleLikeService.createArticleLike(article_id, principal.getName());
        return "redirect:/single/articles/" + article_id;
    }

    @GetMapping("/add/dislike/{id}")
    public String addArticleDislike(@PathVariable("id") Long article_id, Principal principal) {

        articleLikeService.createArticleDislike(article_id, principal.getName());
        return "redirect:/single/articles/" + article_id;
    }

    @GetMapping("/comment/add/like/{id}")
    public String addCommentLike(@PathVariable("id") Long comment_id,
                                 Principal principal) {

        Article article = commentService.findCommentById(comment_id).getArticle();
        commentLikeService.createCommentLike(comment_id, principal.getName(), article);
        return "redirect:/single/articles/" + article.getId();
    }

    @GetMapping("/comment/add/dislike/{id}")
    public String addCommentDislike(@PathVariable("id") Long comment_id,
                                 Principal principal) {

        Article article = commentService.findCommentById(comment_id).getArticle();
        commentLikeService.createCommentDislike(comment_id, principal.getName(), article);
        return "redirect:/single/articles/" + article.getId();
    }
}
