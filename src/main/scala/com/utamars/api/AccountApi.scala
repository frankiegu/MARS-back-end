package com.utamars.api

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import cats.std.all._
import com.github.t3hnar.bcrypt._
import com.utamars.dataaccess._
import com.utamars.util.FacePP

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

case class AccountApi(implicit ec: ExecutionContext, sm: SessMgr, rts: RTS) extends Api {

  override val defaultAuthzRoles = Seq(Role.Admin, Role.Instructor, Role.Assistant)

  override val route =
    (get & path("account") & authnAndAuthz()) { (acc) =>                                                    // Get account info
      complete(Account.findByUsername(acc.username).reply(acc => acc.copy(passwd = "").jsonCompat))
    } ~
    (get & path("account"/"all") & authnAndAuthz(Role.Admin)) { _ =>                                        // Get all account info
      complete(Account.all().reply(accs => Map("accounts" -> accs.map(_.copy(passwd = ""))).jsonCompat))
    } ~
    (get & path("account"/) & netIdsParam & authnAndAuthz(Role.Admin)) { (ids, _) =>                        // Get accounts info by net ids
      complete(Account.findByNetIds(ids.toSet).reply(accs => Map("accounts" -> accs.map(_.copy(passwd = ""))).jsonCompat))
    } ~
    (get & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>                          // Get account info by username
      complete(Account.findByUsername(username).reply(acc => acc.copy(passwd = "").jsonCompat))
    } ~
    (delete & path("account"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) =>                       // Delete account by username
      val result = for {
        acc <- Account.findByUsername(username).leftMap(err2HttpResp)
        _   <- FacePP.personDelete(s"mars_${acc.netId}")
        _   <- Account.deleteByUsername(username).leftMap(err2HttpResp)
      } yield ()

      complete(result.reply(_ => OK))
    } ~
    ((post|put) & path("account"/"change-password") & authnAndAuthz()) { acc =>                             // Change account password
      formField('new_password) { newPass =>
        complete(acc.changePassword(newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-password"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) => // Change account password by username
      formField('new_password) { newPass =>
        complete(Account.changePassword(username, newPass.bcrypt).reply(_ => OK))
      }
    } ~
    ((post|put) & path("account"/"change-approve"/Segment) & authnAndAuthz(Role.Admin)) { (username, _) => // Change account approval by username
      formField('approve.as[Boolean]) { newApprove =>
        complete(Account.findByUsername(username).flatMap(_.copy(approve = newApprove).update()).reply(_ => OK))
      }
    }
}