package com.spreadshirt.continuationtoken

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.stream.Collectors
import java.util.zip.CRC32

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaginationTest {

    @Nested
    inner class testCreatePage {
        @Test
        fun `|1,2,3|4,5,6| different keys`() {
            val allEntries = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4),
                    TestPageable(5),
                    TestPageable(6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2),
                            TestPageable(3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 4)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(4),
                            TestPageable(5),
                            TestPageable(6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,2,3|3,5,6| key 3 overlaps two pages`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 5),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 2),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 4)
            val page2 = createPage(entriesSinceKey, page.token, 4)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 3),
                            TestPageable("5", 5),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = false
            ))
        }

        @Test
        fun `|1,1,1|1,1,1| all have same key`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1),
                    TestPageable("4", 1),
                    TestPageable("5", 1),
                    TestPageable("6", 1)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)
            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 1),
                            TestPageable("3", 1)
                    ),
                    token = ContinuationToken(timestamp = 1, offset = 3, checksum = checksum("1", "2", "3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 1, limit = 6)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 1),
                            TestPageable("5", 1),
                            TestPageable("6", 1)
                    ),
                    token = ContinuationToken(timestamp = 1, offset = 6, checksum = checksum("1", "2", "3", "4", "5", "6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,2,3|| check if token and hasNext is set if entities fit exactly into last page`() {
            val allEntries = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2),
                            TestPageable(3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 3)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = false
            ))
        }

        @Test
        fun `|1,2| hasNext is false if there are less elements than page size`() {
            val allEntries = listOf(
                    TestPageable(1),
                    TestPageable(2)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2)
                    ),
                    token = ContinuationToken(timestamp = 2, offset = 1, checksum = checksum("2")),
                    hasNext = false
            ))
        }

        @Test
        fun `|1,2,3|4| still skip correctly even if there are less elements than page size`() {
            val allEntries = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2),
                            TestPageable(3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 3)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(4)
                    ),
                    token = ContinuationToken(timestamp = 4, offset = 1, checksum = checksum("4")),
                    hasNext = false
            ))
        }

        @Test
        fun `|1,2,3|4,5| second page is not full so no next token`() {
            val allEntries = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4),
                    TestPageable(5)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2),
                            TestPageable(3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 3)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(4),
                            TestPageable(5)
                    ),
                    token = ContinuationToken(timestamp = 5, offset = 1, checksum = checksum(ids = "5")),
                    hasNext = false
            ))
        }

        @Test
        fun `|| empty page`() {
            val page = createPage(listOf(), null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(),
                    token = null,
                    hasNext = false
            ))
        }

        @Test
        fun `|1,3,3|4,5,6|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 3),
                    TestPageable("3", 3),
                    TestPageable("4", 4),
                    TestPageable("5", 5),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 3),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 2, checksum = checksum("2", "3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 5)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 4),
                            TestPageable("5", 5),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,3,3|3,5,6|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 3),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 5),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 3),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 2, checksum = checksum("2", "3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 5)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 3),
                            TestPageable("5", 5),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,3,3|3,3,6|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 3),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 3),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 3),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 2, checksum = checksum("2", "3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 5)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 3),
                            TestPageable("5", 3),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,2,3|3,3,6|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 3),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 2),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 5)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 3),
                            TestPageable("5", 3),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,2,3|4,4,6|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 4),
                    TestPageable("5", 4),
                    TestPageable("6", 6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 2),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 5)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 4),
                            TestPageable("5", 4),
                            TestPageable("6", 6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,1,1|1,1,2|`() {
            val allEntries = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1),
                    TestPageable("4", 1),
                    TestPageable("5", 1),
                    TestPageable("6", 2)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 1),
                            TestPageable("3", 1)
                    ),
                    token = ContinuationToken(timestamp = 1, offset = 3, checksum = checksum("1", "2", "3")),
                    hasNext = true
            ))

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 1, limit = 6)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 1),
                            TestPageable("5", 1),
                            TestPageable("6", 2)
                    ),
                    token = ContinuationToken(timestamp = 2, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))
        }
    }

    @Nested
    inner class testCreatePageWithChecksumFallback {

        @Test
        fun `checksum-pageables modified between pages`() {
            val entriesPage1 = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 3)
            )
            // element with ID was removed
            val entriesPage2 = listOf(
                    TestPageable("3", 3),
                    TestPageable("5", 3),
                    TestPageable("6", 4)
            )

            var page = createPage(entriesPage1, null, 5)
            assertThat(page).isNotNull()
            assertThat(page.token).isNotNull()
            assertThat(page.entities).isEqualTo(entriesPage1)

            // skip element
            page = createPage(entriesPage2, page.token, 5)
            assertThat(page.entities).isEqualTo(entriesPage2)
        }

        @Test
        fun `|1,2,3|4,5,6| id 3 updated and moves to an additional page - different timestamp`() {
            val allEntries = mutableListOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4),
                    TestPageable(5),
                    TestPageable(6)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(1),
                            TestPageable(2),
                            TestPageable(3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            allEntries.updateTimestampOfElement("3", 999)

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 3)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(4),
                            TestPageable(5),
                            TestPageable(6)
                    ),
                    token = ContinuationToken(timestamp = 6, offset = 1, checksum = checksum("6")),
                    hasNext = true
            ))

            val entriesSinceKey2 = allEntries.getEntriesSinceIncluding(timestamp = 6, limit = 4)
            val page3 = createPage(entriesSinceKey2, page.token, 3)
            assertThat(page3).isEqualTo(Page(
                    entities = listOf(
                            TestPageable(6),
                            TestPageable("3", 999)
                    ),
                    token = ContinuationToken(timestamp = 999, offset = 1, checksum = checksum("3")),
                    hasNext = false
            ))
        }

        @Test
        fun `|1,2,3|3,5,6| id 3 updated - timestamp overlaps pages`() {
            val allEntries = mutableListOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 3),
                    TestPageable("5", 5)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 2),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            allEntries.updateTimestampOfElement("3", 999)

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 4)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 3),
                            TestPageable("5", 5),
                            TestPageable("3", 999)
                    ),
                    token = ContinuationToken(timestamp = 999, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,2,3|4,5| id 3 updated - not full page`() {
            val allEntries = mutableListOf(
                    TestPageable("1", 1),
                    TestPageable("2", 2),
                    TestPageable("3", 3),
                    TestPageable("4", 4),
                    TestPageable("5", 5)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 2),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))

            allEntries.updateTimestampOfElement("3", 999)

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 4)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("4", 4),
                            TestPageable("5", 5),
                            TestPageable("3", 999)
                    ),
                    token = ContinuationToken(timestamp = 999, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,1,1|1,1,1| id 3 updated and moves to an additional page - same timestamp`() {
            val allEntries = mutableListOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1),
                    TestPageable("4", 1),
                    TestPageable("5", 1),
                    TestPageable("6", 1)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 1),
                            TestPageable("3", 1)
                    ),
                    token = ContinuationToken(timestamp = 1, offset = 3, checksum = checksum("1", "2", "3")),
                    hasNext = true
            ))

            allEntries.updateTimestampOfElement("3", 999)

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 1, limit = 6)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 1),
                            TestPageable("4", 1),
                            TestPageable("5", 1),
                            TestPageable("6", 1),
                            TestPageable("3", 999)
                    ),
                    token = ContinuationToken(timestamp = 999, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))
        }

        @Test
        fun `|1,3,3|4| id 3 updated - not full page`() {
            val allEntries = mutableListOf(
                    TestPageable("1", 1),
                    TestPageable("2", 3),
                    TestPageable("3", 3),
                    TestPageable("4", 4)
            )
            val firstPage = allEntries.getEntriesSinceIncluding(timestamp = 0, limit = 3)

            val page = createPage(firstPage, null, 3)
            assertThat(page).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("1", 1),
                            TestPageable("2", 3),
                            TestPageable("3", 3)
                    ),
                    token = ContinuationToken(timestamp = 3, offset = 2, checksum = checksum("2", "3")),
                    hasNext = true
            ))

            allEntries.updateTimestampOfElement("3", 999)

            val entriesSinceKey = allEntries.getEntriesSinceIncluding(timestamp = 3, limit = 4)
            val page2 = createPage(entriesSinceKey, page.token, 3)
            assertThat(page2).isEqualTo(Page(
                    entities = listOf(
                            TestPageable("2", 3),
                            TestPageable("4", 4),
                            TestPageable("3", 999)
                    ),
                    token = ContinuationToken(timestamp = 999, offset = 1, checksum = checksum("3")),
                    hasNext = true
            ))
        }

        private fun MutableList<TestPageable>.updateTimestampOfElement(id: String, newTimestamp: Long) {
            val element = this.find { it.getID() == id }
            this.remove(element)
            this.add(TestPageable(id, newTimestamp))
        }
    }

    @Nested
    inner class testCreateToken {
        @Test
        fun `only one entity with highest timestamp`() {
            val pageables = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4)
            )
            val token = createTokenFromEntities(pageables)
            assertThat(token).isEqualTo(ContinuationToken(timestamp = 4, offset = 1, checksum = checksum("4")))
        }

        @Test
        fun `two entities with highest timestamp`() {
            val pageables = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable("3", 3),
                    TestPageable("4", 3)
            )
            val token = createTokenFromEntities(pageables)
            assertThat(token).isEqualTo(ContinuationToken(timestamp = 3, offset = 2, checksum = checksum("3", "4")))
        }

        @Test
        fun `all elements have same timestamp`() {
            val pageables = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1)
            )
            val token = createTokenFromEntities(pageables)
            assertThat(token).isEqualTo(ContinuationToken(timestamp = 1, offset = 3, checksum = checksum("1", "2", "3")))
        }

        @Test
        fun `one element list`() {
            val pageables = listOf(
                    TestPageable(1)
            )
            val token = createTokenFromEntities(pageables)
            assertThat(token).isEqualTo(ContinuationToken(timestamp = 1, offset = 1, checksum = checksum("1")))
        }

        @Test
        fun `empty list`() {
            val token = createTokenFromEntities(listOf())
            assertThat(token).isNull()
        }

        @Test
        fun `varying pagesize with unequal timestamps`() {
            val pageables = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3),
                    TestPageable(4),
                    TestPageable(5),
                    TestPageable(6)
            )
            var page = createPage(pageables.slice(0..1), null, 2)
            assertThat(page.token).isNotNull()
            assertThat(page.entities).hasSize(2)
            page = createPage(pageables.slice(2..4), page.token, 3)
            assertThat(page.token).isNotNull()
            assertThat(page.entities).hasSize(3)
            page = createPage(pageables.slice(5..pageables.size - 1), page.token, 3)
            assertThat(page.entities.first()).isEqualToComparingFieldByField(pageables.last())
        }

        @Test
        fun `offset is larger than page size`() {
            val pageables = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1)
            )

            var page = createPage(pageables.slice(0..1), null, 2)
            page = createPage(pageables, page.token, 1)
            assertThat(page.token).isNotNull()
            assertThat(page.entities).hasSize(1)
            assertThat(page.entities.first()).isEqualToComparingFieldByField(pageables.last())
        }
    }

    @Nested
    inner class testCalculateQueryAdvice {
        @Test
        fun `no token provided`() {
            val advice = calculateQueryAdvice(token = null, pageSize = 5)
            assertThat(advice).isEqualTo(QueryAdvice(timestamp = 0, limit = 5))
        }

        @Test
        fun `there was one element with timestamp 20 in the last page`() {
            val token = ContinuationToken(timestamp = 20, offset = 1, checksum = 999)
            val advice = calculateQueryAdvice(token, pageSize = 5)
            assertThat(advice).isEqualTo(QueryAdvice(timestamp = 20, limit = 6))
        }

        @Test
        fun `there were 3 elements with timestamp 20 in the last page`() {
            val token = ContinuationToken(timestamp = 20, offset = 3, checksum = 999)
            val advice = calculateQueryAdvice(token, pageSize = 5)
            assertThat(advice).isEqualTo(QueryAdvice(timestamp = 20, limit = 8))
        }
    }

    @Nested
    inner class testGetEntitiesWithHighestKey {
        @Test
        fun `all have different keys`() {
            val pageables = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable(3)
            )
            val entities = getLatestEntities(pageables)
            assertThat(entities).containsExactly(TestPageable(3))
        }

        @Test
        fun `some with the same key`() {
            val pageables = listOf(
                    TestPageable(1),
                    TestPageable(2),
                    TestPageable("4", 3),
                    TestPageable("5", 3)
            )
            val entities = getLatestEntities(pageables)
            assertThat(entities).containsExactly(TestPageable("4", 3), TestPageable("5", 3))
        }

        @Test
        fun `all with the same key`() {
            val pageables = listOf(
                    TestPageable("1", 1),
                    TestPageable("2", 1),
                    TestPageable("3", 1)
            )
            val entities = getLatestEntities(pageables)
            assertThat(entities).containsExactly(TestPageable("1", 1), TestPageable("2", 1), TestPageable("3", 1))
        }

        @Test
        fun `empty list`() {
            val entities = getLatestEntities(listOf())
            assertThat(entities).isEmpty()
        }

        @Test
        fun `only one element`() {
            val pageables = listOf(TestPageable(1))
            val entities = getLatestEntities(pageables)
            assertThat(entities).containsExactly(TestPageable("1", 1))
        }
    }

    @Test
    fun `validate sort order`() {
        val actual = listOf(
                // reversed with equal timestamp
                TestPageable("1", 4),
                TestPageable("2", 3),
                TestPageable("2", 2),
                TestPageable("4", 1),
                // equal timestamp but different id
                TestPageable("5", 5),
                TestPageable("4", 5)
        )
        val expected = listOf(
                TestPageable("4", 1),
                TestPageable("2", 2),
                TestPageable("2", 3),
                TestPageable("1", 4),
                TestPageable("4", 5),
                TestPageable("5", 5)
        )
        val sorted = actual.stream()
                .sorted(::compareByDateModifiedAndIdAscending)
                .collect(Collectors.toList())
        assertThat(sorted).isEqualTo(expected)
    }

    private fun List<Pageable>.getEntriesSinceIncluding(timestamp: Int, limit: Int)
            = this.filter { it.getTimestamp() >= timestamp }.take(limit)
}

private fun checksum(vararg ids: String): Long {
    val hash = CRC32()
    hash.update(ids.joinToString("_").toByteArray())
    return hash.value
}

data class TestPageable(
        private val id: String,
        private val timestamp: Long
) : Pageable {
    constructor(timestamp: Long) : this(timestamp.toString(), timestamp)

    override fun getID() = id
    override fun getTimestamp() = timestamp
}