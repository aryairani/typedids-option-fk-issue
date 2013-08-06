    package controllers

    import model._
    import scala.slick.session.Database.threadLocalSession
    import com.typesafe.slick.driver.oracle.OracleDriver.simple._


    object Auth {
      def auth(username: String, password: String): Boolean = {
        val q = for {
          (u,s) <- User leftJoin Company on (_.agencyId is _.id)
          if u.name.toLowerCase === username.toLowerCase.trim && u.password === fn_hash(password)
        } yield (s.dataOutOfDate.?.getOrElse(false))

        db withSession {
          q.list.map {
            case (agencyOutOfDate) => !agencyOutOfDate
          }.headOption.getOrElse(false)
        }
      }
    }