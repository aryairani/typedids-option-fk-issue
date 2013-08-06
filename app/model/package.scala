    import play.api.db.DB
    import play.api.Play.current // implicit Application
    import scala.slick.session.Database.threadLocalSession // needed for `withSession`
    import scala.slick.lifted.MappedTypeMapper
    import com.typesafe.slick.driver.oracle.OracleDriver.simple._

    package object model {
      def db = Database.forDataSource(DB.getDataSource())


    /* Row and Table helpers */
      trait RowId[T <: TypedId] { def id: Option[T]}
      trait RowName  { def name: String    }
      trait RowIdName[T <: TypedId] extends RowId[T] with RowName

      abstract class TableId[T <: TypedId : TypeMapper, R <: RowId[T]](tblName: String) extends Table[R](tblName) {
        def id = column[T]("ID", O.PrimaryKey)
        def byId = createFinderBy(_.id) // TODO: test me
      }

      abstract class TableIdName[R <: RowId[T], T <: TypedId : TypeMapper](tblName: String) extends TableId[T,R](tblName) {
        def name = column[String]("NAME")
        def byName = createFinderBy(_.name)

        def idName = id ~ name
        def optIdName = id.? ~ name

        def sortedIdName = this.map(_.idName).sortBy(_._2)
      }



    /* Custom ID types */
      type RawID = Array[Byte]
      trait TypedId extends Any {
        def untypedId: RawID
        override def toString = untypedId.map("%02x" format _).mkString
      }
      case class CompanyId(untypedId: RawID) extends AnyVal with TypedId
      case class UserId(untypedId: RawID) extends AnyVal with TypedId


    /* Custom ID type mappers */
      sealed trait IdFactory[T <: TypedId] extends (RawID => T)

      implicit object CompanyId        extends IdFactory[CompanyId]
      implicit object UserId          extends IdFactory[UserId]

      implicit def idMapper[T <: TypedId : IdFactory]: TypeMapper[T] =
        MappedTypeMapper.base[T, RawID](_.untypedId, implicitly[IdFactory[T]])


    /* Tables & Queries */
      case class User(id: Option[UserId] = None,
                      name: String,
                      password: String,
                      agencyId: Option[CompanyId] = None,
                      inactive: Boolean = false,
                      disabled: Boolean = true
                       ) extends RowIdName[UserId] {
        lazy val agency: Option[Company] = db withSession agencyId.map(Company.byId.first)
      }

      case class LoginQuery(userId: UserId, agencyId: Option[CompanyId], disabled: Boolean, inactive: Boolean)

      object User extends TableIdName[User,UserId]("USER") {
        def password = column[String]("PASSWORD")
        def agencyId = column[Option[CompanyId]]("COMPANY_ID")//(typeMapperToOptionTypeMapper(idMapper(AgencyId)))
        def inactive = column[Boolean]("INACTIVE", O.Default(false))
        def disabled = column[Boolean]("IS_DISABLED", O.Default(true))

        def * = optIdName ~ password ~ agencyId ~ inactive ~ disabled <> (User.apply _, User.unapply _)
        def loginQuery = id ~ agencyId ~ disabled ~ inactive <> (LoginQuery.apply _, LoginQuery.unapply _)

        def find(name: String) = db withSession byName(name).firstOption

        def agency = foreignKey("FK_TB_USER_AGENCY_ID",agencyId,Company)(_.id.?)
      }

      case class Company(id: Option[CompanyId] = None, name: String, dataOutOfDate: Boolean = false) extends RowIdName[CompanyId]

      object Company extends TableIdName[Company,CompanyId]("COMPANY") {
        def dataOutOfDate = column[Boolean]("IS_DATA_OUT_OF_DATE")
        def * = id.? ~ name ~ dataOutOfDate <> (Company.apply _, Company.unapply _)
      }


    /* Remote functions */
      val fn_hash = SimpleFunction.unary[String,String]("fn_hash")

    }
