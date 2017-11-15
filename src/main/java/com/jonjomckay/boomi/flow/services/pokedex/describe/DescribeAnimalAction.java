package com.jonjomckay.boomi.flow.services.pokedex.describe;

import com.manywho.sdk.api.ContentType;
import com.manywho.sdk.services.actions.Action;

@Action.Metadata(name = "Describe Animal", summary = "Find some information about a specific animal", uri = "describe/animal")
public class DescribeAnimalAction implements Action {
    public static class Input {
        @Action.Input(name = "Animal", contentType = ContentType.String)
        private String animal;

        public String getAnimal() {
            return animal;
        }
    }

    public static class Output {
        @Action.Output(name = "Description", contentType = ContentType.String)
        private String description;

        @Action.Output(name = "Image", contentType = ContentType.String)
        private String image;

        public Output() {
        }

        public Output(String description, String image) {
            this.description = description;
            this.image = image;
        }

        public String getDescription() {
            return description;
        }

        public String getImage() {
            return image;
        }
    }
}
