package net.sprd.demo.pagination

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.sprd.demo.pagination.util.DesignCreator
import net.sprd.demo.pagination.util.FunctionsMySQL
import org.assertj.core.api.Assertions.assertThat
import org.h2.jdbcx.JdbcDataSource
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DesignResourceTest {

    private val resource = initDesignResource()
    private val creator = DesignCreator(dataSource)

    @Test
    fun `page through two pages`() {
        val startDate = Instant.ofEpochSecond(1512757070)
        creator.createDesigns(amount = 5, startDate = startDate)

        val firstPageResponse = resource.getDesigns(Request(Method.GET, "/designs?pageSize=3"))

        val firstPage = firstPageResponse.toPageDTO()
        assertThat(firstPage.continuationToken).isNotNull()
        assertThat(firstPage.nextPage).isNotNull()
        assertThat(firstPage.designs).containsExactly(
                DesignDTO(id = "0", title = "Cat 0", imageUrl = "http://domain.de/cat0.jpg", dateModified = 1512757070)
                , DesignDTO(id = "1", title = "Cat 1", imageUrl = "http://domain.de/cat1.jpg", dateModified = 1512757071)
                , DesignDTO(id = "2", title = "Cat 2", imageUrl = "http://domain.de/cat2.jpg", dateModified = 1512757072)
        )

        val secondPageResponse = resource.getDesigns(Request(Method.GET, firstPage.nextPage!!))
        val secondPage = secondPageResponse.toPageDTO()
        assertThat(secondPage.continuationToken).isNull()
        assertThat(secondPage.nextPage).isNull()
        assertThat(secondPage.designs).containsExactly(
                DesignDTO(id = "3", title = "Cat 3", imageUrl = "http://domain.de/cat3.jpg", dateModified = 1512757073)
                , DesignDTO(id = "4", title = "Cat 4", imageUrl = "http://domain.de/cat4.jpg", dateModified = 1512757074)
        )
    }

    private fun initDesignResource(): DesignResource {
        val dao = DesignDAO(dataSource)
        FunctionsMySQL.register(dataSource.connection)
        ScriptUtils.executeSqlScript(dataSource.connection, ClassPathResource("create-designs-table.sql"))
        return DesignResource(dao)
    }
}

private val dataSource = JdbcDataSource().apply {
    user = "sa"
    password = ""
    setURL("jdbc:h2:mem:access;MODE=MySQL;DB_CLOSE_DELAY=-1")
}

private val mapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

private fun Response.toPageDTO() = mapper.readValue(bodyString(), PageDTO::class.java)

