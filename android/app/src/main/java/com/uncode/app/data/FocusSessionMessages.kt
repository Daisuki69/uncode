package com.uncode.app.data

object FocusSessionMessages {
    val quotes = listOf(
        "You could've been so much further by now.",
        "You know what's embarrassing? knowing exactly what you need to do and still not doing it.",
        "Is avoiding work your actual talent?",
        "Pascal built a fucking calculator at 18. what have you done today?",
        "Some people your age are changing the world. you're still fighting {random deckname}.",
        "Newton was figuring out calculus. you're figuring out how to avoid studying.",
        "People have done extraordinary shit at your age. you can't even finish one deck.",
        "Imagine having access to all this knowledge and still choosing to do nothing.",
        "You call yourself smart? prove it. answer the fucking questions.",
        "Do you actually know this or are you just good at pretending?",
        "What if you're not as smart as you think you are?",
        "How much do you actually know without looking at your notes?",
        "Maybe you're not ready. have you considered that?",
        "Your classmates are studying btw. just thought you should know.",
        "Someone in your class is probably studying right now. embarrassing for you.",
        "Your rival is studying. again. must be nice having actual discipline.",
        "They're getting ahead while you're sitting on your ass.",
        "Imagine getting outworked by someone you thought you were better than.",
        "Your classmates aren't even worried about you anymore 💀",
        "They're probably reviewing the next chapter while you're still here.",
        "Smartest person in your imagination maybe",
        "All that ego and you still don't know the basics",
        "Maybe shut up about being smart and actually study",
        "Your confidence needs to be studied more than this deck",
        "You've got a lot of confidence for someone who knows so little"
    )

    fun getRandomQuote(fallbackDeckName: String = "your flashcards"): String {
        return quotes.random().replace("{random deckname}", fallbackDeckName)
    }
}

