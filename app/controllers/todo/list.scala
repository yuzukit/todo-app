package controllers.todo

import javax.inject._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._ 

import model.ViewValueTodo
import lib.persistence.default.TodoRepository
import lib.persistence.default.TodoCategoryRepository
import lib.model.{Todo, TodoCategory}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.i18n.I18nSupport

//import scala.concurrent.ExecutionContext.Implicits.global

case class TodoFormData(category_name: String, title: String, body: String, state: Todo.Status)

@Singleton
class TodoController @Inject()(
  val controllerComponents: ControllerComponents
  )(implicit val ec: ExecutionContext) extends BaseController 
  with I18nSupport {

  // Todo登録用のFormオブジェクト
  val form = Form(
    // html formのnameがcontentのものを140文字以下の必須文字列に設定する
    mapping(
      "category_name" -> nonEmptyText,
      "title"         -> nonEmptyText(maxLength = 140),
      "body"          -> nonEmptyText(maxLength = 140),
      "state"         -> default     (of[Todo.Status], Todo.Status.IS_UNTOUCHED)
    )(TodoFormData.apply)(TodoFormData.unapply)
  )

  def index() = Action async { implicit req =>
    for {
      todoSeq     <- TodoRepository.getall()
      categorySeq <- TodoCategoryRepository.getall()
    } yield {
      val res = todoSeq.map(todo => (
          todo.id.get,
          todo.title, 
          todo.body, 
          todo.state, 
          categorySeq.filter(_.id.get == todo.category_id).head.name))

      Ok(views.html.todo.list(
        ViewValueTodo(
          title  = "タスク一覧",
          cssSrc = Seq("main.css"),
          jsSrc  = Seq("main.js"),
          data   = res
        ),

      ))
    }
  }

  /**
    * 登録画面の表示用
    */
  def register() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.todo.store(form))
  }

  /**
    * 登録処理実を行う
    */
  def store() = Action async { implicit request: Request[AnyContent] =>
     // foldでデータ受け取りの成功、失敗を分岐しつつ処理が行える
    form.bindFromRequest().fold(
       // 処理が失敗した場合に呼び出される関数
       (formWithErrors: Form[TodoFormData]) => {
         Future.successful(BadRequest(views.html.todo.store(formWithErrors)))
       },
       // 処理が成功した場合に呼び出される関数
       (todoFormData: TodoFormData) => {
         for {
            categorySeq <- TodoCategoryRepository.getall()
           // データを登録。returnのidは不要なので捨てる
            _ <- TodoRepository.add(Todo(
              //None, 
              categorySeq.filter(_.name == todoFormData.category_name).head.id.get,
              todoFormData.title, 
              todoFormData.body, 
              ))
         } yield {
           Redirect(routes.TodoController.index())
         }
       }
    )
  }

  /**
    * 編集画面を開く
    */
  def edit(id: Long) = Action async { implicit request: Request[AnyContent] =>
    for {
      todoSeq     <- TodoRepository.getall()
      categorySeq <- TodoCategoryRepository.getall()
   } yield {
      val todoOpt = todoSeq.filter(_.id.get == Todo.Id(id)).headOption
      todoOpt match {
          case Some(todo: Todo) =>
            Ok(views.html.todo.edit(
            // データを識別するためのidを渡す
            Todo.Id(id),
            // fillでformに値を詰める
            form.fill(TodoFormData(
              categorySeq.filter(_.id.get == todo.category_id).head.name,
              todo.title,
              todo.body,
              todo.state
            ))
          ))
          case None        =>
            NotFound(views.html.error.page404())
     }
   }
  }

  /**
    * 対象のツイートを更新する
    */
  def update(id: Long) = Action async { implicit request: Request[AnyContent] =>
    form.bindFromRequest().fold(
      (formWithErrors: Form[TodoFormData]) => {
        Future.successful(BadRequest(views.html.todo.edit(Todo.Id(id), formWithErrors)))
      },
      (data: TodoFormData) => {
        for {
          categorySeq   <- TodoCategoryRepository.getall()
          oldTodoEntity <- TodoRepository.get(Todo.Id(id))
          count         <- TodoRepository.update(
            oldTodoEntity.get.map(_.copy(
              category_id = categorySeq.filter(_.name == data.category_name).head.id.get,
              title       = data.title,
              body        = data.body,
              state       = data.state,
            ))
          )
        } yield {
          count match {
            //case 0 => NotFound(views.html.error.page404())
            case _ => Redirect(routes.TodoController.index())
          }
        }
      }
      )
  }

  /**
   * 対象のデータを削除する
   */
  def delete() = Action async { implicit request: Request[AnyContent] =>
    // requestから直接値を取得するサンプル
    val idOpt = request.body.asFormUrlEncoded.get("id").headOption
    for {
      result <- TodoRepository.remove(Todo.Id(idOpt.get.toLong))
    } yield {
      // 削除対象の有無によって処理を分岐
      result match {
        //case 0 => NotFound(views.html.error.page404())
        case _ => Redirect(routes.TodoController.index())
      }
    }
  }
}