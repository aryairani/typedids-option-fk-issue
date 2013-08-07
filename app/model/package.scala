import play.api.db.DB
import play.api.Play.current // implicit Application
import scala.slick.lifted.MappedTypeMapper
import com.typesafe.slick.driver.oracle.OracleDriver.simple._

package object model {
  def db = Database.forDataSource(DB.getDataSource())


/* Custom ID types */
  type RawID = Array[Byte]
  trait TypedId extends Any {
    def untypedId: RawID
    override def toString = untypedId.map("%02x" format _).mkString
  }
  case class AgencyId(untypedId: RawID) extends AnyVal with TypedId
  case class UserId(untypedId: RawID) extends AnyVal with TypedId


/* Custom ID type mappers */
  sealed trait IdFactory[T <: TypedId] extends (RawID => T)

  implicit object AgencyId        extends IdFactory[AgencyId]
  implicit object UserId          extends IdFactory[UserId]

  implicit def idMapper[T <: TypedId : IdFactory]: TypeMapper[T] =
    MappedTypeMapper.base[T, RawID](_.untypedId, implicitly[IdFactory[T]])


/* Tables & Queries */
  case class User(id: Option[UserId] = None,
                  name: String,
                  password: String,
                  agencyId: Option[AgencyId] = None,
                  inactive: Boolean = false,
                  disabled: Boolean = true)

  object User extends Table[User]("USER") {
    def id = column[UserId]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def password = column[String]("PASSWORD")
    def agencyId = column[Option[AgencyId]]("AGENCY_ID")//(typeMapperToOptionTypeMapper(idMapper(AgencyId)))
    def inactive = column[Boolean]("INACTIVE", O.Default(false))
    def disabled = column[Boolean]("IS_DISABLED", O.Default(true))

    def * = id.? ~ name ~ password ~ agencyId ~ inactive ~ disabled <> (User.apply _, User.unapply _)

    def agency = foreignKey("FK_TB_USER_AGENCY_ID",agencyId,Agency)(_.id.?)
  }

  case class Agency(id: Option[AgencyId] = None, name: String, dataOutOfDate: Boolean = false)

  object Agency extends Table[Agency]("AGENCY") {
    def id = column[AgencyId]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def dataOutOfDate = column[Boolean]("IS_DATA_OUT_OF_DATE")
    def * = id.? ~ name ~ dataOutOfDate <> (Agency.apply _, Agency.unapply _)
  }


/* Remote functions */
  val fn_hash = SimpleFunction.unary[String,String]("fn_hash")

}
