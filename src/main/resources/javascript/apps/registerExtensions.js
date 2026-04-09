window.jahia.i18n.loadNamespaces('macros-ckeditor5-config');

window.jahia.uiExtender.registry.add('callback', 'macros-ckeditor5-config', {
    targets: ['jahiaApp-init:99.5'],
    callback: function () {
        const items = [
            {id: '##authorname', name: 'Author name', text: '##authorname##'},
            {id: '##creationdate', name: 'Creation date', text: '##creationdate##'},
            {id: '##keywords', name: 'Keywords', text: '##keywords##'},
            {id: '##linktohomepage', name: 'Link to home page', text: '##linktohomepage##'},
            {id: '##linktoparent', name: 'Link to parent', text: '##linktoparent##'},
            {id: '##requestParameters', name: 'Request parameters', text: '##requestParameters##'},
            {id: '##resourceBundle', name: 'Resource bundle', text: '##resourceBundle(key, bundleName)##'},
            {id: '##username', name: 'User name', text: '##username##'}
        ];

        function customItemRenderer(item) {
            const itemElement = document.createElement('div');
            itemElement.id = `macros-item-${item.id.replace(/#/g, '')}`;
            itemElement.textContent = `${item.name} `;

            const macroElement = document.createElement('small');
            macroElement.style.cssText = 'color: inherit; font-size: 10px;';
            macroElement.textContent = item.text;

            itemElement.appendChild(macroElement);

            return itemElement;
        }

        try {
            console.debug('Register Macros for CKEditor5');

            const complete = window.jahia.uiExtender.registry.get('ckeditor5-config', 'complete');
            complete.mention = {
                feeds: [
                    {
                        marker: '##',
                        feed: items,
                        itemRenderer: customItemRenderer
                    }
                ]
            };
        } catch (e) {
            console.error('Error setting up macros configuration', e);
        }
    }
});
