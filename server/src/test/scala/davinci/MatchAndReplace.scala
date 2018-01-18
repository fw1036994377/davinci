
package davinci

import edp.davinci.KV
import edp.davinci.rest.RouteHelper._
import edp.davinci.util.common.DavinciConstants.{STEndChar, STStartChar, sqlSeparator}
import edp.davinci.util.common.{RegexMatch, STRender}
import edp.davinci.util.sql.SqlParser
import org.scalatest.FunSuite
import org.stringtemplate.v4.{ST, STGroupString}
import edp.davinci.util.json.JsonUtils.json2caseClass

class MatchAndReplace extends FunSuite {
  test("expression map") {
    val expressionList = List("name = <v1<", "city in ($v2$)", "age >=10", "sex != '男'", "age < 20")
    val expressionMap = SqlParser.getParsedMap(expressionList)
    expressionMap.foreach(e => {
      println(e._2._1)
      println(e._1)
      e._2._2.foreach(println)
      println("~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    })
    assert("" == "")
  }

  test("get expression list") {
    val str = "Is is the cost of of gasoline going up up where ((name_) = <v1_<) and (city =<v2<) and (age > <v3<) or sex != '男'"
    val regex = "\\(<^\\<]*\\<\\w+\\<\\s?\\)"
    val expressionList = RegexMatch.getMatchedItemList(str, regex)
    expressionList.foreach(println)
    val exprList = List("((name_) = <v1_<)", "(city =<v2<)", "(age > <v3<)")
    assert(exprList == expressionList, "this is right what i want")
  }

  test("match all") {
    val groupStr = "[{\"k\":\"v3\",\"v\":\"24\"},{\"k\":\"v3\",\"v\":\"45\"}]"
    val queryStr = "[{\"k\":\"v1\",\"v\":\"liaog\"}]"
    val flatTableSqls =
      """group@var $v1$ = mary;
        |group@var $v2$ = 'beijing','shanghai';
        |query@var $v3$ = 20;
        |query@var $v4$ = select * from table;
        |query@var $v5$ = '女';
        |
        |
        |query@var $date$='2017';
        |query@var $fromdate$;
        |update@var $todate$;
        |
        |        
        |{
        |    $if(date)$
        |        select * from table where (name = $v1$) and (city in ($v2$)) and age > $v3$ or sex != $v5$;
        |    $elseif(todate)$
        |        select a, b, c from table1 where a = $v5$ and b = $v5$;
        |    $elseif(fromdate)$
        |        afafjajfhjaf;
        |    $else$
        |        select 拒贷码,人数,CONCAT(round(人数/总数 *100,2),'%') as 占比 from table1 where a <> $v3$ and b = $v5$ and c = 5;
        |    $endif$
        |    adadaada
        |}""".stripMargin

    val groupParams = json2caseClass[Seq[KV]](groupStr)
    val queryParams = json2caseClass[Seq[KV]](queryStr)

    val trimSql = flatTableSqls.trim
    println("the initial sql template:" + trimSql)
    val sqls = if (trimSql.lastIndexOf(sqlSeparator) == trimSql.length - 1) trimSql.dropRight(1).split(sqlSeparator) else trimSql.split(sqlSeparator)
    val sqlWithoutVar = trimSql.substring(trimSql.indexOf(STStartChar) + 1, trimSql.indexOf(STEndChar)).trim
    val groupKVMap = getGroupKVMap(sqls, groupParams)
    val queryKVMap = getQueryKVMap(sqls, queryParams)
    val mergeSql = RegexMatch.matchAndReplace(sqlWithoutVar, groupKVMap)
    val renderedSql = if (queryKVMap.nonEmpty) STRender.renderSql(mergeSql, queryKVMap) else mergeSql
    println("sql:" + renderedSql)
  }


  test("unit test") {
    val templates =
      """delimiters "%", "%"
        |method(name) ::= <<
        |%stat(name)%
        |>>
        |stat(name,value="99") ::= "x=%value%; // %name%"
        |
      """.stripMargin
    val group = new STGroupString(templates)
    val b = group.getInstanceOf("method")
    b.add("name", "foo")
    val expecting = "x=99; // foo"
    val result = b.render
    println(expecting == result)
  }


  test("template") {

    val queryStr = "[]"
    val flatTableSqls =
      """query@var $startdate$;
        |query@var $enddate$;
        |{SELECT DATE_FORMAT(createtime, '%Y-%m-%d')  as 注册日期, COUNT( * ) as 注册数量 FROM ec_carowner where 1=1 $if(startdate)$ and DATE_FORMAT(createtime, '%Y-%m-%d')>=$startdate$ and DATE_FORMAT(createtime, '%Y-%m-%d')<=$enddate$ $endif$ GROUP BY DATE_FORMAT(createtime, '%Y-%m-%d') order by DATE_FORMAT(createtime, '%Y-%m-%d')}""".stripMargin


    val queryParams = json2caseClass[Seq[KV]](queryStr)

    val trimSql = flatTableSqls.trim
    println("the initial sql template:" + trimSql)
    val sqls = if (trimSql.lastIndexOf(sqlSeparator) == trimSql.length - 1) trimSql.dropRight(1).split(sqlSeparator) else trimSql.split(sqlSeparator)
    val sqlWithoutVar = trimSql.substring(trimSql.indexOf(STStartChar) + 1, trimSql.indexOf(STEndChar)).trim
    val groupKVMap = getGroupKVMap(sqls, null)
    val queryKVMap = getQueryKVMap(sqls, queryParams)
    val mergeSql = RegexMatch.matchAndReplace(sqlWithoutVar, groupKVMap)
    val renderedSql = STRender.renderSql(mergeSql, queryKVMap)
    println("sql:" + renderedSql)
  }


  test("ST"){
    val template = "hi <name><if(a)> asdajh <endif>!"
    val st = new ST(template)
    val expected = "hi !"
    val result = st.render()
    println(result+">>>>>>>>")
   println(result==expected)
  }

}
