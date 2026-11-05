package com.widgy.lyrics.domain.engine

import com.widgy.lyrics.data.api.LyricsCleaner
import com.widgy.lyrics.data.db.DailyHistoryDao
import com.widgy.lyrics.data.db.DailyHistoryEntity
import com.widgy.lyrics.data.db.SnippetDao
import com.widgy.lyrics.data.db.SongDao
import com.widgy.lyrics.domain.model.Candidate
import com.widgy.lyrics.domain.model.DailyLyric
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DailyRotationEngine(
    private val songDao: SongDao,
    private val snippetDao: SnippetDao,
    private val dailyHistoryDao: DailyHistoryDao,
    private val seed: Long = 42L
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun buildCandidatePool(): List<Candidate> {
        val candidates = mutableListOf<Candidate>()

        val activeSnippets = snippetDao.getActiveSnippets()
        for (snippet in activeSnippets) {
            val song = songDao.getSongById(snippet.songId)
            if (song != null) {
                candidates.add(Candidate.Curated(snippet, song))
            }
        }

        val randomSongs = songDao.getRandomPoolSongs()
        for (song in randomSongs) {
            candidates.add(Candidate.RandomSong(song))
        }

        return candidates
    }

    suspend fun resolveDailyLyric(date: LocalDate, forceRefresh: Boolean = false): DailyLyric? {
        val dateString = date.format(dateFormatter)
        val existing = dailyHistoryDao.getDailyLyric(dateString)

        if (!forceRefresh && existing != null) {
            return DailyLyric(
                date = existing.date,
                songId = existing.songId,
                snippetId = existing.snippetId,
                text = existing.lyricText,
                songTitle = existing.songTitle,
                artistName = existing.artistName
            )
        }

        val candidates = buildCandidatePool()
        if (candidates.isEmpty()) {
            return null
        }

        val selectedCandidate = if (forceRefresh) {
            if (candidates.size > 1) {
                val filtered = candidates.filter { candidate ->
                    when (candidate) {
                        is Candidate.Curated -> candidate.snippet.content != existing?.lyricText
                        is Candidate.RandomSong -> candidate.song.id != existing?.songId
                    }
                }
                if (filtered.isNotEmpty()) filtered.random() else candidates.random()
            } else {
                candidates.first()
            }
        } else {
            val epochDay = date.toEpochDay()
            val rawHash = (epochDay * 31 + seed).hashCode().toLong()
            val index = (rawHash and 0x7FFFFFFFL).toInt() % candidates.size
            candidates[index]
        }

        val resolved = when (selectedCandidate) {
            is Candidate.Curated -> {
                val text = if (forceRefresh && candidates.size == 1 && selectedCandidate.snippet.content == existing?.lyricText) {
                    val cleaned = LyricsCleaner.cleanLyrics(selectedCandidate.song.plainLyrics)
                    val alternate = cleaned.filter { it.text != existing?.lyricText }
                    if (alternate.isNotEmpty()) {
                        val randIdx = Random.nextInt(alternate.size)
                        val lineCount = if (alternate.size > 1 && Random.nextBoolean()) 2 else 1
                        val endIdx = (randIdx + lineCount).coerceAtMost(alternate.size)
                        alternate.subList(randIdx, endIdx).joinToString("\n") { it.text }
                    } else {
                        selectedCandidate.snippet.content
                    }
                } else {
                    selectedCandidate.snippet.content
                }

                DailyLyric(
                    date = dateString,
                    songId = selectedCandidate.song.id,
                    snippetId = selectedCandidate.snippet.id,
                    text = text,
                    songTitle = selectedCandidate.song.title,
                    artistName = selectedCandidate.song.artist
                )
            }
            is Candidate.RandomSong -> {
                val cleanedLines = LyricsCleaner.cleanLyrics(selectedCandidate.song.plainLyrics)
                val lineText = if (cleanedLines.isNotEmpty()) {
                    if (forceRefresh) {
                        val nonMatching = cleanedLines.filter { it.text != existing?.lyricText }
                        if (nonMatching.isNotEmpty()) {
                            val randIdx = Random.nextInt(nonMatching.size)
                            val lineCount = if (nonMatching.size > 1 && Random.nextBoolean()) 2 else 1
                            val endIdx = (randIdx + lineCount).coerceAtMost(nonMatching.size)
                            nonMatching.subList(randIdx, endIdx).joinToString("\n") { it.text }
                        } else {
                            cleanedLines.random().text
                        }
                    } else {
                        val epochDay = date.toEpochDay()
                        val rawHash = (epochDay * 17 + seed).hashCode().toLong()
                        val startIndex = (rawHash and 0x7FFFFFFFL).toInt() % cleanedLines.size
                        val lineCount = if (cleanedLines.size > 1 && (epochDay % 2L == 0L)) 2 else 1
                        val endIndex = (startIndex + lineCount).coerceAtMost(cleanedLines.size)
                        cleanedLines.subList(startIndex, endIndex).joinToString("\n") { it.text }
                    }
                } else {
                    selectedCandidate.song.title
                }

                DailyLyric(
                    date = dateString,
                    songId = selectedCandidate.song.id,
                    snippetId = null,
                    text = lineText,
                    songTitle = selectedCandidate.song.title,
                    artistName = selectedCandidate.song.artist
                )
            }
        }

        dailyHistoryDao.insertOrUpdate(
            DailyHistoryEntity(
                date = resolved.date,
                songId = resolved.songId,
                snippetId = resolved.snippetId,
                lyricText = resolved.text,
                songTitle = resolved.songTitle,
                artistName = resolved.artistName,
                updatedAt = System.currentTimeMillis()
            )
        )

        return resolved
    }
}
