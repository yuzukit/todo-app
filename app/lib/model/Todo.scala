/**
  * New file.
  * 
  */

package lib.model

import ixias.model._
import ixias.util.EnumStatus

import java.time.LocalDateTime

// ユーザーを表すモデル
//~~~~~~~~~~~~~~~~~~~~
import Todo._
case class Todo(
  id:          Option[Id],
  category_id: TodoCategory.Id,
  title:       String,
  body:        String,
  state:       Status,
  updatedAt:   LocalDateTime = NOW,
  createdAt:   LocalDateTime = NOW
) extends EntityModel[Todo.Id]

// コンパニオンオブジェクト
//~~~~~~~~~~~~~~~~~~~~~~~~
object Todo {

  val  Id = the[Identity[Id]]
  type Id = Long @@ Todo
  type WithNoId   = Entity.WithNoId  [Id, Todo]
  type EmbeddedId = Entity.EmbeddedId[Id, Todo]

  // ステータス定義
  //~~~~~~~~~~~~~~~~~
  sealed abstract class Status(val code: Short) extends EnumStatus
  object Status extends EnumStatus.Of[Status] {
    //type Status = values
    case object IS_UNTOUCHED extends Status(code = 0) //未着手
    case object IS_ONGOING   extends Status(code = 1) //進行中
    case object IS_FINISHED  extends Status(code = 2) //完了
  }

  // INSERT時のIDがAutoincrementのため,IDなしであることを示すオブジェクトに変換
  def apply(category_id: TodoCategory.Id, title: String, body: String, state: Status): WithNoId = {
    new Entity.WithNoId(
      new Todo(
        id          = None,
        category_id = category_id,
        title       = title,
        body        = body,
        state       = state,
      )
    )
  }

}