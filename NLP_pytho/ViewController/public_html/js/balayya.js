

function handleEnterKey(event) {

    if (event.getKeyCode() == 13) {

        // Enter key

        var sendButton = AdfPage.PAGE.findComponent('r1:0:sendButton');

        if (sendButton) {

            AdfActionEvent.queue(sendButton, true);

        }

        event.cancel();

    }

}

function handleKey(evt) {

 

    var inputTextComponent = evt.getSource();

 

    AdfLogger.LOGGER.logMessage(AdfLogger.INFO, "Event: " + evt);

 

    if (evt.getKeyCode() == AdfKeyStroke.ENTER_KEY) {

        AdfLogger.LOGGER.logMessage(AdfLogger.INFO, "Component: " + inputTextComponent);

 

        AdfCustomEvent.queue(inputTextComponent, "handleKey",

 

        {

            fvalue : inputTextComponent.getSubmittedValue()

 

        },

        false);

 

        evt.cancel();

 

    }

 

}

 

function clearInput() {

    var inputField = AdfPage.PAGE.findComponent('userInputField');

    if (inputField) {

        inputField.setValue('');

    }

}

 

// Auto-scroll to bottom of chat area

function scrollToBottom() {

    var chatArea = document.getElementById('chatResponseArea');

    if (chatArea) {

        chatArea.scrollTop = chatArea.scrollHeight;

    }

}

AdfPage.PAGE.addBusyStateListener(function (event) {

    if (!event.isBusy()) {

        setTimeout(scrollToBottom, 100);

    }

});

// Floating animated background letters effect
(function() {
    var letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
    var container = document.createElement('div');
    container.id = 'floating-letters-bg';
    container.style.position = 'fixed';
    container.style.top = '0';
    container.style.left = '0';
    container.style.width = '100vw';
    container.style.height = '100vh';
    container.style.pointerEvents = 'none';
    container.style.zIndex = '0';
    container.style.overflow = 'hidden';
    document.body.appendChild(container);

    function randomBetween(a, b) {
        return Math.random() * (b - a) + a;
    }

    function createLetter() {
        var span = document.createElement('span');
        span.textContent = letters[Math.floor(Math.random() * letters.length)];
        span.style.position = 'absolute';
        span.style.left = randomBetween(0, 98) + 'vw';
        span.style.top = randomBetween(0, 95) + 'vh';
        span.style.fontSize = randomBetween(16, 28) + 'px';
        span.style.color = 'rgba(180,180,180,0.13)';
        span.style.fontWeight = '400';
        span.style.userSelect = 'none';
        span.style.transition = 'transform 8s linear, opacity 8s linear';
        container.appendChild(span);
        // Animate
        setTimeout(function() {
            span.style.transform = 'translateY(' + randomBetween(-40, 40) + 'px) translateX(' + randomBetween(-40, 40) + 'px)';
            span.style.opacity = '0';
        }, 100);
        // Remove after animation
        setTimeout(function() {
            container.removeChild(span);
        }, 8000);
    }

    // Add 40 floating letters at random intervals
    setInterval(function() {
        if (container.childNodes.length < 40) {
            createLetter();
        }
    }, 350);
})();

// Typewriter effect for bot messages
function typeBotMessage() {
    // Find the latest bot message that is not yet typed
    var botMessages = document.querySelectorAll('.bot-message');
    if (!botMessages.length) return;
    var lastBot = botMessages[botMessages.length - 1];
    if (lastBot.classList.contains('typed')) return; // Already typed
    var contentElem = lastBot.querySelector('.chat-response-text, .af_outputFormatted');
    if (!contentElem) return;
    var fullText = contentElem.innerHTML;
    contentElem.innerHTML = '';
    lastBot.classList.add('typed');
    var i = 0;
    function typeChar() {
        if (i <= fullText.length) {
            contentElem.innerHTML = fullText.substring(0, i);
            i++;
            setTimeout(typeChar, 25);
        } else {
            contentElem.innerHTML = fullText;
        }
    }
    typeChar();
}

// Call typeBotMessage after scrollToBottom (after chat updates)
function scrollToBottom() {
    var chatArea = document.getElementById('chatResponseArea');
    if (chatArea) {
        chatArea.scrollTop = chatArea.scrollHeight;
    }
    setTimeout(typeBotMessage, 200); // Add typewriter after scroll
}