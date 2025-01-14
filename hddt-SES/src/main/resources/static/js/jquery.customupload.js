(function($) {
    $.fn.customupload = function(options) {

        var defaults = {
            urlNoImage: "assets/imgs/no-image.jpg",
            typeAccept: ["image/jpeg", "image/png", "image/jpg"],
            sizeAccept: 1024 * 5
        };

        var settings = $.extend({}, defaults, options);

        var selecter = this;
        var fileType = "";
        var uploadContainer = selecter.closest("div.upload-img-container");
        var errorDiv = uploadContainer.find("#message-box-error");

        //var imgPreviewDiv = selecter.closest("div.upload-img-preview");
        var removeImg = uploadContainer.find("#remove-img");
        var imgPreview = uploadContainer.find("#preview-img");

        //change
        selecter.change(function() {
            errorDiv.hide(); // To remove the previous error message
            var file = this.files[0];
            fileType = file.type;
            if (file.size > (settings.sizeAccept * 1024)) {
                alert("File size must be less than " + settings.sizeAccept + "Kb");
                return false;
            }
            var checkTypeAcept = false;
            $.each(settings.typeAccept,
                function(index, value) {
                    if (fileType == value) {
                        checkTypeAcept = true;
                        return false;
                    }
                });

            if (!checkTypeAcept) {
                errorDiv.show().find("span.message")
                    .html("Only " + settings.typeAccept.toString() + " images type allowed");
                return false;
            } else {
                var reader = new FileReader();
                reader.onload = selecter.imageIsLoaded;
                return reader.readAsDataURL(file);
            }
        });

        selecter.imageIsLoaded = function(e) {
            imgPreview.attr("src", e.target.result);
            imgPreview.next("input").val(e.target.result.replace("data:" + fileType + ";base64,", ""));
            imgPreview.next("input").next("input").val(fileType);
            imgPreview.show().attr("data-type", "1");
            removeImg.removeClass('d-none');
        };

        removeImg.on("click",
            function(event) {
                event.preventDefault();
                imgPreview.attr("src", settings.urlNoImage);
                imgPreview.next("input").val("DELETE");
                removeImg.removeClass('d-none').addClass('d-none');
                selecter.val("");
            });
    };
})(jQuery);