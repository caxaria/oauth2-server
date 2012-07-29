package org.scalatra
package oauth2

import command._
import model.{BCryptPassword, Account, fieldNames}
import OAuth2Imports._
import util.conversion.TypeConverter
import _root_.scalaz._
import Scalaz._
import collection.generic.{Shrinkable, Growable}
import net.liftweb.json._
import net.liftweb.json

package object commands {

  def get[C <: OAuth2Command](oauth: OAuth2Extension)(implicit mf: Manifest[C]): C = {
    val const = mf.erasure.getConstructor(classOf[OAuth2Extension])
    const.newInstance(oauth).asInstanceOf[C]
  }

  abstract class OAuth2Command(val oauth: OAuth2Extension) extends Command with ValidationSupport with CommandValidators with Growable[Command] with Shrinkable[Command] {

    implicit def toValidator[T: Zero](fn: T ⇒ FieldValidation[T]): Validator[T] = {
      case s ⇒ fn(~s)
    }

    implicit val stringToWebDate: TypeConverter[DateTime] = DateFormats.parse _

    implicit val stringToObjectId: TypeConverter[ObjectId] = safe(new ObjectId(_))

    implicit val stringToBCryptPassword: TypeConverter[BCryptPassword] = safeOption(BCryptPassword.parse)

    def +=(elem: Command): OAuth2Command = {
      bindings :::= elem.bindings
      this
    }

    def clear() {
      bindings = Seq.empty[Binding[_]]
    }

    def -=(elem: Command): OAuth2Command = {
      bindings = bindings filterNot (_ == elem)
      this
    }
  }

  trait OAuth2CommandPart {
    val oauth: OAuth2Extension
  }

  trait IdCommand[ID <: Any] { self: OAuth2Command ⇒

    lazy val retrieved: FieldValidation[Account] = {
      val r = id.converted.flatMap(oauth.userProvider.findOneById(_))
      r some (_.success) none FieldError("Not found").fail
    }

    def id: ValidatedBinding[ID]

  }


}